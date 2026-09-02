package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.agent.Agent;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Human-in-the-loop ReACT agent. The graph is compiled with
 * {@code interruptBefore("action")}, so every agent turn parks before the
 * action node — but that alone is NOT an approval gate: the agent→action edge
 * is unconditional, so tool-free runs park there too (issue #247).
 *
 * <p>A genuine approval gate requires the parked state AND a pending tool
 * proposal (last AI message carrying {@code ToolExecutionRequest}s). Tool-free
 * parks are auto-completed silently; approvals resume via
 * {@code stream(null, config)} which continues at the checkpoint's next node
 * instead of starting a new run; rejections never touch the graph, so
 * unapproved tools can never execute.</p>
 *
 * <p>The graph and stream/answer helpers are provided by {@link
 * AgentGraphFactory} (issue T1); checkpoint memory is bounded via the shared
 * {@link BoundedMemorySaver} (issue #252).</p>
 */
@Service
public class HumanInTheLoopService {

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    /** Upper bound on the number of rejected-session ids we remember. */
    private static final int MAX_REJECTED_SESSIONS = 1000;

    /**
     * Session ids whose pending action was rejected. Kept bounded (issue #265)
     * so a later start/resume cannot re-execute the unapproved action; the
     * in-memory marker is enough because these are short-lived conversations.
     * A LinkedHashMap-as-set (keys) evicts the oldest rejected id first.
     */
    private final Map<String, Boolean> rejectedSessions =
            Collections.synchronizedMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_REJECTED_SESSIONS;
                }
            });

    public HumanInTheLoopService(AgentGraphFactory factory, BaseCheckpointSaver checkpointSaver)
            throws GraphStateException {
        this.compiledGraph = factory.humanInTheLoop(checkpointSaver);
    }

    public HitlResult start(String sessionId, String task) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        if (rejectedSessions.containsKey(sessionId)) {
            // The session's pending action was rejected (#265): a fresh start
            // must not resume the parked graph and execute the unapproved tools.
            return new HitlResult(sessionId, task, "(rejected earlier)", List.of(),
                    false, "", "");
        }

        List<String> steps = new ArrayList<>();
        execute(sessionId, Map.of("messages", UserMessage.from(task)), steps);

        return buildResult(sessionId, task, steps, null);
    }

    /**
     * Continues a paused run. Approval resumes the graph at the checkpoint's
     * next node (executing the approved tools); rejection terminates the
     * session without touching it — unapproved tools never execute.
     */
    public HitlResult resume(String sessionId, boolean approved, String feedback) {
        if (rejectedSessions.containsKey(sessionId)) {
            // A rejection was already recorded (#265); never re-execute tools on
            // this session regardless of the new approval flag.
            return new HitlResult(sessionId, "(rejected: " + feedback + ")", "(rejected earlier)",
                    List.of(), false, "", feedback);
        }
        if (!approved) {
            // Resuming would execute the unapproved tools (the agent->action
            // edge is unconditional), so rejection ends the session here and
            // records it so a later start/resume cannot execute them (#265).
            String answer = compiledGraph.lastStateOf(configFor(sessionId))
                    .map(snapshot -> AgentGraphFactory.answerOf(snapshot.state()))
                    .orElse("No response");
            rejectedSessions.put(sessionId, Boolean.TRUE);
            return new HitlResult(sessionId, "(rejected: " + feedback + ")", answer,
                    List.of(), false, "", feedback);
        }

        List<String> steps = new ArrayList<>();
        execute(sessionId, null, steps);

        return buildResult(sessionId, "(approved)", steps, feedback);
    }

    private void execute(String sessionId, Map<String, Object> input, List<String> steps) {
        RunnableConfig config = configFor(sessionId);
        // null input means GraphInput.resume(): continue at the checkpoint's
        // next node. An empty map would start a NEW run from the entrypoint.
        AgentGraphFactory.GraphRun<AgentExecutor.State> run =
                AgentGraphFactory.stream(compiledGraph, input, config);
        steps.addAll(run.steps());
    }

    /**
     * A pending action exists only when the graph is parked before the action
     * node AND the last AI message carries unapproved tool requests.
     */
    public Optional<PendingAction> getPendingAction(String sessionId) {
        RunnableConfig config = configFor(sessionId);
        var snapshotOpt = compiledGraph.stateOf(config);
        if (snapshotOpt.isEmpty() || !Agent.ACTION_LABEL.equals(snapshotOpt.get().next())) {
            return Optional.empty();
        }
        AiMessage lastAi = lastAiMessageWithRequests(snapshotOpt.get().state());
        if (lastAi == null) {
            return Optional.empty();
        }
        StringBuilder proposed = new StringBuilder();
        for (var req : lastAi.toolExecutionRequests()) {
            if (!proposed.isEmpty()) {
                proposed.append("\n");
            }
            proposed.append("tool_call: ").append(req.name()).append("(").append(req.arguments()).append(")");
        }
        return Optional.of(new PendingAction(sessionId, proposed.toString()));
    }

    private RunnableConfig configFor(String sessionId) {
        return RunnableConfig.builder().threadId(sessionId).build();
    }

    private static AiMessage lastAiMessageWithRequests(AgentExecutor.State state) {
        List<ChatMessage> messages = state.messages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (m instanceof AiMessage ai) {
                return ai.hasToolExecutionRequests() ? ai : null;
            }
        }
        return null;
    }

    private HitlResult buildResult(String sessionId, String task, List<String> steps, String feedback) {
        return buildResult(sessionId, task, steps, feedback, true);
    }

    private HitlResult buildResult(String sessionId, String task, List<String> steps,
                                   String feedback, boolean autoCompleteParks) {
        var pending = getPendingAction(sessionId);

        if (pending.isEmpty() && autoCompleteParks) {
            // The run parked before "action" with nothing to approve: drive it
            // to END so the session reaches a terminal state. Nothing executes.
            execute(sessionId, null, steps);
            pending = getPendingAction(sessionId);
        }

        boolean awaitingApproval = pending.isPresent();
        String answer = "";
        if (!awaitingApproval) {
            answer = compiledGraph.lastStateOf(configFor(sessionId))
                    .map(snapshot -> AgentGraphFactory.answerOf(snapshot.state()))
                    .orElse("No response");
        }

        return new HitlResult(sessionId, task, answer, steps, awaitingApproval,
                pending.map(PendingAction::proposedAction).orElse(""), feedback);
    }

    public record HitlResult(
            String sessionId,
            String task,
            String answer,
            List<String> steps,
            boolean awaitingApproval,
            String proposedAction,
            String feedback
    ) {}

    public record PendingAction(
            String sessionId,
            String proposedAction
    ) {}
}
