package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 */
@Service
public class StatefulPipelineService {

    private static final int MAX_ITERATIONS = 8;

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    /** Session id -> thread id of that session's most recent run. */
    private final Map<String, String> latestThreadBySession = new ConcurrentHashMap<>();

    public StatefulPipelineService(ChatModel chatModel,
                                   CalculatorTool calculatorTool,
                                   DocumentSearchTool documentSearchTool,
                                   WeatherTool weatherTool,
                                   EmbeddingStoreStatsTool storeStatsTool) throws GraphStateException {
        StateGraph<AgentExecutor.State> graph = AgentExecutor.builder()
                .chatModel(chatModel)
                .systemMessage(SystemMessage.from(ReactAgentService.SYSTEM_MESSAGE))
                .toolsFromObject(calculatorTool, documentSearchTool, weatherTool, storeStatsTool)
                .build();

        this.compiledGraph = graph.compile(
                CompileConfig.builder()
                        .checkpointSaver(new MemorySaver())
                        .recursionLimit(MAX_ITERATIONS * 2)
                        .build());
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

        List<String> steps = new ArrayList<>();
        AgentExecutor.State lastState = null;

        var generator = compiledGraph.stream(Map.of("messages", input), config);
        try {
            for (var item : generator) {
                String nodeName = item.node();
                if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                    steps.add(nodeName);
                }
                lastState = item.state();
            }
        } catch (RuntimeException e) {
            if (lastState == null && steps.isEmpty()) {
                throw e;
            }
        }

        latestThreadBySession.put(sessionId, threadId);

        String answer = ReactAgentService.answerOf(lastState);

        // Fall back to the persisted checkpoint state when the stream produced nothing usable.
        if (answer.equals("No response")) {
            lastState = compiledGraph.lastStateOf(config)
                    .map(StateSnapshot::state)
                    .orElse(null);
            answer = ReactAgentService.answerOf(lastState);
        }

        List<StateEntry> history = getStateHistory(threadId);

        return new StatefulResult(sessionId, task, answer, steps, history.size(), history);
    }

    private List<ChatMessage> priorMessages(String sessionId) {
        String previousThread = latestThreadBySession.get(sessionId);
        if (previousThread == null) {
            return List.of();
        }
        return compiledGraph.lastStateOf(
                        RunnableConfig.builder().threadId(previousThread).build())
                .map(snapshot -> snapshot.state().messages())
                .orElse(List.of());
    }

    public List<StateEntry> getStateHistory(String sessionId) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(sessionId)
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
                    snapshot.config().threadId().orElse(sessionId),
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
            String sessionId,
            String node,
            String lastAiMessage,
            int messageCount
    ) {}
}
