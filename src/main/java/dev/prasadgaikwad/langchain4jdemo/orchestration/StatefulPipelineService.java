package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
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

@Service
public class StatefulPipelineService {

    private static final int MAX_ITERATIONS = 8;

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

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
                        .recursionLimit(MAX_ITERATIONS)
                        .build());
    }

    public StatefulResult run(String sessionId, String task) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        final String threadId = sessionId;

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        List<String> steps = new ArrayList<>();
        AgentExecutor.State lastState = null;

        var generator = compiledGraph.stream(Map.of("messages", UserMessage.from(task)), config);
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

        String answer = ReactAgentService.answerOf(lastState);

        // Fall back to the persisted checkpoint state when the stream produced nothing usable.
        if (answer.equals("No response")) {
            lastState = compiledGraph.lastStateOf(config)
                    .map(StateSnapshot::state)
                    .orElse(null);
            answer = ReactAgentService.answerOf(lastState);
        }

        List<StateEntry> history = getStateHistory(threadId);

        return new StatefulResult(threadId, task, answer, steps, history.size(), history);
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
