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

    private static final int MAX_ITERATIONS = 8;

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    public HumanInTheLoopService(ChatModel chatModel,
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
                        .interruptBefore(Agent.ACTION_LABEL)
                        .recursionLimit(MAX_ITERATIONS * 2)
                        .build());
    }

    public HitlResult start(String sessionId, String task) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        List<String> steps = new ArrayList<>();
        execute(sessionId, Map.of("messages", UserMessage.from(task)), steps);

        return buildResult(sessionId, task, steps, null);
    }

    public HitlResult resume(String sessionId, boolean approved, String feedback) {
        List<String> steps = new ArrayList<>();
        // Empty input + same thread ID resumes from the saved checkpoint.
        execute(sessionId, Map.of(), steps);

        return buildResult(sessionId, approved ? "(approved)" : "(rejected: " + feedback + ")", steps, feedback);
    }

    private void execute(String sessionId, Map<String, Object> input, List<String> steps) {
        RunnableConfig config = configFor(sessionId);
        var generator = compiledGraph.stream(input, config);
        try {
            for (var item : generator) {
                String nodeName = item.node();
                if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                    steps.add(nodeName);
                }
            }
        } catch (RuntimeException e) {
            if (steps.isEmpty()) {
                throw e;
            }
        }
    }

    public Optional<PendingAction> getPendingAction(String sessionId) {
        var snapshot = compiledGraph.stateOf(configFor(sessionId));
        if (snapshot.isEmpty() || !Agent.ACTION_LABEL.equals(snapshot.get().next())) {
            return Optional.empty();
        }
        AgentExecutor.State state = snapshot.get().state();
        String lastAiMessage = state.messages().stream()
                .filter(m -> m instanceof AiMessage ai && ai.text() != null && !ai.text().isBlank())
                .map(m -> ((AiMessage) m).text())
                .reduce((first, second) -> second)
                .orElseGet(() -> state.messages().stream()
                        .filter(m -> m instanceof AiMessage ai && ai.hasToolExecutionRequests())
                        .map(m -> "tool call proposed")
                        .reduce((first, second) -> second)
                        .orElse(""));
        return Optional.of(new PendingAction(sessionId, lastAiMessage));
    }

    private RunnableConfig configFor(String sessionId) {
        return RunnableConfig.builder().threadId(sessionId).build();
    }

    /**
     * Reads the outcome from the persisted checkpoint state — never re-invokes
     * the graph. When the graph is paused at the interrupt the run is still
     * awaiting approval; otherwise the final answer comes from the last state.
     */
    private HitlResult buildResult(String sessionId, String task, List<String> steps, String feedback) {
        var pending = getPendingAction(sessionId);
        boolean awaitingApproval = pending.isPresent();
        String answer = "";
        if (!awaitingApproval) {
            answer = compiledGraph.lastStateOf(configFor(sessionId))
                    .map(snapshot -> ReactAgentService.answerOf(snapshot.state()))
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
