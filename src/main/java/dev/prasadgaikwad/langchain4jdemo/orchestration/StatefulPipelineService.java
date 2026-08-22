package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class StatefulPipelineService {

    private final CompiledGraph<AgentExecutor.State> compiledGraph;
    private final MemorySaver memorySaver;

    public StatefulPipelineService(ChatModel chatModel,
                                   CalculatorTool calculatorTool,
                                   DocumentSearchTool documentSearchTool,
                                   WeatherTool weatherTool,
                                   EmbeddingStoreStatsTool storeStatsTool) throws GraphStateException {
        this.memorySaver = new MemorySaver();

        StateGraph<AgentExecutor.State> graph = AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(calculatorTool, documentSearchTool, weatherTool, storeStatsTool)
                .build();

        this.compiledGraph = graph.compile(
                CompileConfig.builder()
                        .checkpointSaver(memorySaver)
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

        var generator = compiledGraph.stream(Map.of("messages", UserMessage.from(task)), config);
        for (var item : generator) {
            String nodeName = item.node();
            if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                steps.add(nodeName);
            }
        }

        Optional<AgentExecutor.State> finalState = compiledGraph.invoke(
                Map.of("messages", UserMessage.from(task)), config);

        String answer = finalState
                .flatMap(AgentExecutor.State::finalResponse)
                .orElse("No response");

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
            List<String> messages = state.messages().stream()
                    .filter(m -> m instanceof AiMessage)
                    .map(m -> ((AiMessage) m).text())
                    .toList();
            String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1);
            entries.add(new StateEntry(
                    snapshot.config().threadId().orElse(sessionId),
                    snapshot.node(),
                    lastMessage,
                    messages.size()));
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
