package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
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
import org.bsc.langgraph4j.agent.Agent;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class HumanInTheLoopService {

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    public HumanInTheLoopService(ChatModel chatModel,
                                 CalculatorTool calculatorTool,
                                 DocumentSearchTool documentSearchTool,
                                 WeatherTool weatherTool,
                                 EmbeddingStoreStatsTool storeStatsTool) throws GraphStateException {
        StateGraph<AgentExecutor.State> graph = AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(calculatorTool, documentSearchTool, weatherTool, storeStatsTool)
                .build();

        this.compiledGraph = graph.compile(
                CompileConfig.builder()
                        .checkpointSaver(new MemorySaver())
                        .interruptBefore(Agent.ACTION_LABEL)
                        .build());
    }

    public HitlResult start(String sessionId, String task) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        RunnableConfig config = configFor(sessionId);
        List<String> steps = new ArrayList<>();

        var generator = compiledGraph.stream(Map.of("messages", UserMessage.from(task)), config);
        for (var item : generator) {
            String nodeName = item.node();
            if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                steps.add(nodeName);
            }
        }

        return buildResult(sessionId, task, steps, null);
    }

    public HitlResult resume(String sessionId, boolean approved, String feedback) {
        RunnableConfig config = configFor(sessionId);

        List<String> steps = new ArrayList<>();
        var generator = compiledGraph.stream(Map.of(), config);
        for (var item : generator) {
            String nodeName = item.node();
            if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                steps.add(nodeName);
            }
        }

        return buildResult(sessionId, approved ? "(approved)" : "(rejected: " + feedback + ")", steps, feedback);
    }

    public Optional<PendingAction> getPendingAction(String sessionId) {
        var snapshot = compiledGraph.stateOf(configFor(sessionId));
        if (snapshot.isEmpty() || !"action".equals(snapshot.get().next())) {
            return Optional.empty();
        }
        AgentExecutor.State state = snapshot.get().state();
        String lastAiMessage = state.messages().stream()
                .filter(m -> m instanceof AiMessage)
                .map(m -> ((AiMessage) m).text())
                .reduce((first, second) -> second)
                .orElse("");
        return Optional.of(new PendingAction(sessionId, lastAiMessage));
    }

    private RunnableConfig configFor(String sessionId) {
        return RunnableConfig.builder().threadId(sessionId).build();
    }

    private HitlResult buildResult(String sessionId, String task, List<String> steps, String feedback) {
        var pending = getPendingAction(sessionId);
        boolean awaitingApproval = pending.isPresent();
        String answer = "";
        if (!awaitingApproval) {
            var state = compiledGraph.invoke(Map.of(), configFor(sessionId));
            answer = state.flatMap(AgentExecutor.State::finalResponse).orElse("No response");
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
