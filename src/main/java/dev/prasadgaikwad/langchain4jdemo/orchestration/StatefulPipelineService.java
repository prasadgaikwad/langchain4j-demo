package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ReACT agent with conversational sessions over langgraph4j checkpoints.
 *
 * <p>Each {@link #run} executes on a <b>fresh graph thread</b>
 * ({@code sessionId:runUuid}). The persisted {@code agent_response} value
 * short-circuits AgentExecutor's tool node straight to END once it is set, so
 * reusing one thread across turns froze every later answer at the first
 * turn's text (issue #249). Conversation continuity is preserved instead by
 * seeding each new run's {@code messages} channel with the previous run's
 * transcript plus the new user message.</p>
 *
 * <p>The graph and stream/answer helpers are provided by {@link
 * AgentGraphFactory} (issue T1); checkpoint memory is bounded via the shared
 * {@link BoundedMemorySaver} (issue #252).</p>
 */
@Service
public class StatefulPipelineService {

    /** Upper bound on the number of sessions tracked for continuity seeding. */
    private static final int MAX_TRACKED_SESSIONS = 1000;

    private final CompiledGraph<AgentExecutor.State> compiledGraph;
    private final int maxContextMessages;

    /** Session id -> thread id of that session's most recent run. */
    private final Map<String, String> latestThreadBySession;

    public StatefulPipelineService(AgentGraphFactory factory, BaseCheckpointSaver checkpointSaver)
            throws GraphStateException {
        this(factory, checkpointSaver, 20);
    }

    @Autowired
    public StatefulPipelineService(AgentGraphFactory factory, BaseCheckpointSaver checkpointSaver,
                                   @Value("${app.stateful.max-context-messages:20}") int maxContextMessages)
            throws GraphStateException {
        this.compiledGraph = factory.checkpointed(checkpointSaver);
        this.latestThreadBySession = createBoundedSessionMap();
        this.maxContextMessages = maxContextMessages;
    }

    private static Map<String, String> createBoundedSessionMap() {
        // Bounded so a long-running server cannot grow the continuity map without
        // bound (issue #252). Evicts the oldest-inserted session first. Access is
        // low-frequency, so a synchronized insertion-ordered map is sufficient.
        return Collections.synchronizedMap(new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_TRACKED_SESSIONS;
            }
        });
    }

    public StatefulResult run(String sessionId, String task) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        // A fresh thread starts with agent_response unset, so this run gets a
        // complete ReACT loop (tool execution -> agent synthesis).
        final String threadId = sessionId + ":" + UUID.randomUUID();

        List<ChatMessage> input = new ArrayList<>(priorMessages(sessionId));
        input.add(UserMessage.from(task));

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        AgentGraphFactory.GraphRun<AgentExecutor.State> run =
                AgentGraphFactory.stream(compiledGraph, Map.of("messages", input), config);

        latestThreadBySession.put(sessionId, threadId);

        AgentExecutor.State lastState = run.lastState();
        String answer = AgentGraphFactory.answerOf(lastState);

        // Fall back to the persisted checkpoint state when the stream produced nothing usable.
        if (answer.equals("No response")) {
            lastState = compiledGraph.lastStateOf(config)
                    .map(StateSnapshot::state)
                    .orElse(null);
            answer = AgentGraphFactory.answerOf(lastState);
        }

        List<StateEntry> history = getStateHistory(threadId);

        return new StatefulResult(sessionId, task, answer, run.steps(), history.size(), history);
    }

    private List<ChatMessage> priorMessages(String sessionId) {
        String previousThread = latestThreadBySession.get(sessionId);
        if (previousThread == null) {
            return List.of();
        }
        List<ChatMessage> messages = compiledGraph.lastStateOf(
                        RunnableConfig.builder().threadId(previousThread).build())
                .map(snapshot -> snapshot.state().messages())
                .orElse(List.of());
        // Slide a window over the prior transcript so a long session cannot grow
        // the seeded prompt without bound (issue #259). Keep the most recent
        // messages (plus the new user message added by the caller).
        if (messages.size() <= maxContextMessages) {
            return messages;
        }
        return new ArrayList<>(messages.subList(messages.size() - maxContextMessages, messages.size()));
    }

    public List<StateEntry> getStateHistory(String threadId) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        Collection<StateSnapshot<AgentExecutor.State>> snapshots =
                compiledGraph.getStateHistory(config);

        List<StateEntry> entries = new ArrayList<>();
        for (StateSnapshot<AgentExecutor.State> snapshot : snapshots) {
            AgentExecutor.State state = snapshot.state();
            String lastMessage = state.messages().stream()
                    .filter(m -> m instanceof AiMessage ai && ai.text() != null && !ai.text().isBlank())
                    .map(m -> ((AiMessage) m).text())
                    .reduce((first, second) -> second)
                    .orElse("");
            entries.add(new StateEntry(
                    snapshot.config().threadId().orElse(threadId),
                    snapshot.node(),
                    lastMessage,
                    state.messages().size()));
        }
        return entries;
    }

    public record StatefulResult(
            String sessionId,
            String task,
            String answer,
            List<String> steps,
            int checkpointCount,
            List<StateEntry> history
    ) {}

    public record StateEntry(
            String threadId,
            String node,
            String lastAiMessage,
            int messageCount
    ) {}
}
