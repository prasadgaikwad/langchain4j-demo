package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single source of truth for the ReACT {@link AgentExecutor} graph shared by
 * {@link ReactAgentService}, {@link StatefulPipelineService}, and
 * {@link HumanInTheLoopService}. Centralizes the identical system message,
 * tool wiring, compile configuration, and the stream/answer/trace helpers that
 * the three services previously duplicated (issue #251 / audit T1).
 *
 * <p>The two knobs all three services vary — checkpoint persistence and
 * pre-action interrupts — are provided by the {@code react()}, {@code
 * checkpointed(...)}, and {@code humanInTheLoop(...)} factory methods.</p>
 */
@Component
public class AgentGraphFactory {

    static final String SYSTEM_MESSAGE = """
            You are a helpful assistant with access to tools. Rules:
            - If a tool call fails, do NOT retry the same tool more than once. Move on and answer with what you have.
            - Prefer answering directly over calling tools repeatedly.
            """;

    /** Default recursion budget shared by all three graphs (issue #260). */
    static final int DEFAULT_RECURSION_LIMIT = 16;

    private final ChatModel chatModel;
    private final CalculatorTool calculatorTool;
    private final DocumentSearchTool documentSearchTool;
    private final WeatherTool weatherTool;
    private final EmbeddingStoreStatsTool storeStatsTool;
    private final int recursionLimit;

    public AgentGraphFactory(ChatModel chatModel,
                             CalculatorTool calculatorTool,
                             DocumentSearchTool documentSearchTool,
                             WeatherTool weatherTool,
                             EmbeddingStoreStatsTool storeStatsTool) {
        this(chatModel, calculatorTool, documentSearchTool, weatherTool, storeStatsTool,
                DEFAULT_RECURSION_LIMIT);
    }

    @Autowired
    public AgentGraphFactory(ChatModel chatModel,
                             CalculatorTool calculatorTool,
                             DocumentSearchTool documentSearchTool,
                             WeatherTool weatherTool,
                             EmbeddingStoreStatsTool storeStatsTool,
                             @Value("${app.agent.recursion-limit:16}") int recursionLimit) {
        this.chatModel = chatModel;
        this.calculatorTool = calculatorTool;
        this.documentSearchTool = documentSearchTool;
        this.weatherTool = weatherTool;
        this.storeStatsTool = storeStatsTool;
        this.recursionLimit = recursionLimit;
    }

    /** The plain ReACT graph: no checkpoints, shared recursion budget. */
    public CompiledGraph<AgentExecutor.State> react() throws GraphStateException {
        return graph(null, List.of());
    }

    /** A checkpointed graph with no pre-action interrupt (conversational stateful turns). */
    public CompiledGraph<AgentExecutor.State> checkpointed(BaseCheckpointSaver saver) throws GraphStateException {
        return graph(saver, List.of());
    }

    /** A checkpointed graph that parks before every action for human approval. */
    public CompiledGraph<AgentExecutor.State> humanInTheLoop(BaseCheckpointSaver saver) throws GraphStateException {
        return graph(saver, List.of(Agent.ACTION_LABEL));
    }

    private CompiledGraph<AgentExecutor.State> graph(BaseCheckpointSaver saver,
                                                     List<String> interruptBefore) throws GraphStateException {
        StateGraph<AgentExecutor.State> stateGraph = AgentExecutor.builder()
                .chatModel(chatModel)
                .systemMessage(SystemMessage.from(SYSTEM_MESSAGE))
                .toolsFromObject(calculatorTool, documentSearchTool, weatherTool, storeStatsTool)
                .build();

        CompileConfig.Builder builder = CompileConfig.builder().recursionLimit(recursionLimit);
        if (saver != null) {
            builder.checkpointSaver(saver);
        }
        for (String node : interruptBefore) {
            builder.interruptBefore(node);
        }
        return stateGraph.compile(builder.build());
    }

    /**
     * Streams a run, collecting the ordered step trace and the last observed
     * state. A failure mid-run is always propagated: a partial state is not
     * a terminal state, so returning it as success would hide the error from
     * callers (issue #261). Interrupt-based pauses (HITL) end the stream
     * normally, so they are unaffected.
     *
     * @param input  {@code null} means resume from the checkpoint's next node
     * @param config {@code null} for a single-shot run without a thread
     */
    public static <S extends AgentState> GraphRun<S> stream(
            CompiledGraph<S> graph, Map<String, Object> input, RunnableConfig config) {
        List<String> steps = new ArrayList<>();
        S lastState = null;
        var generator = config == null ? graph.stream(input) : graph.stream(input, config);
        for (var item : generator) {
            String nodeName = item.node();
            if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                steps.add(nodeName);
            }
            lastState = item.state();
        }
        return new GraphRun<>(List.copyOf(steps), lastState);
    }

    static String answerOf(AgentExecutor.State state) {
        if (state == null) {
            return "No response";
        }
        // executeTool writes this sentinel into agent_response when a run ends
        // without tool requests and without a final answer — not a real answer.
        var finalResponse = state.finalResponse()
                .filter(text -> !"no tool execution request found!".equals(text));
        return finalResponse
                .or(() -> lastTextOf(state))
                .orElse("No response");
    }

    private static Optional<String> lastTextOf(AgentExecutor.State state) {
        return state.messages().stream()
                .filter(m -> m instanceof AiMessage ai && ai.text() != null && !ai.text().isBlank())
                .map(m -> ((AiMessage) m).text())
                .reduce((first, second) -> second);
    }

    /**
     * Builds a human-readable ReACT trace from the message history: AI reasoning
     * text, tool calls (name + arguments), and tool results. Pure tool-call
     * AiMessages have no text, which is why naive {@code text()} mapping produced
     * nulls.
     */
    static List<String> traceOf(AgentExecutor.State state) {
        if (state == null) {
            return List.of();
        }
        List<String> trace = new ArrayList<>();
        for (ChatMessage m : state.messages()) {
            if (m instanceof AiMessage ai) {
                if (ai.text() != null && !ai.text().isBlank()) {
                    trace.add(ai.text());
                }
                for (var req : ai.toolExecutionRequests()) {
                    trace.add("tool_call: " + req.name() + "(" + req.arguments() + ")");
                }
            } else if (m instanceof ToolExecutionResultMessage result) {
                trace.add("tool_result[" + result.toolName() + "]: " + result.text());
            }
        }
        return List.copyOf(trace);
    }

    /** The ordered step trace and last state produced by a run. */
    public record GraphRun<S>(List<String> steps, S lastState) {}
}
