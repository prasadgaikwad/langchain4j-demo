package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReactAgentService {

    static final String SYSTEM_MESSAGE = """
            You are a helpful assistant with access to tools. Rules:
            - If a tool call fails, do NOT retry the same tool more than once. Move on and answer with what you have.
            - Prefer answering directly over calling tools repeatedly.
            """;

    private static final int MAX_ITERATIONS = 8;

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    public ReactAgentService(ChatModel chatModel,
                             CalculatorTool calculatorTool,
                             DocumentSearchTool documentSearchTool,
                             WeatherTool weatherTool,
                             EmbeddingStoreStatsTool storeStatsTool) throws GraphStateException {
        StateGraph<AgentExecutor.State> graph = AgentExecutor.builder()
                .chatModel(chatModel)
                .systemMessage(SystemMessage.from(SYSTEM_MESSAGE))
                .toolsFromObject(calculatorTool, documentSearchTool, weatherTool, storeStatsTool)
                .build();

        this.compiledGraph = graph.compile(
                CompileConfig.builder().recursionLimit(MAX_ITERATIONS).build());
    }

    public ReactResult run(String task) {
        List<String> steps = new ArrayList<>();
        AgentExecutor.State lastState = null;

        var generator = compiledGraph.stream(Map.of("messages", UserMessage.from(task)));
        try {
            for (var item : generator) {
                String nodeName = item.node();
                if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                    steps.add(nodeName);
                }
                lastState = item.state();
            }
        } catch (RuntimeException e) {
            if (lastState == null) {
                throw e;
            }
        }

        String answer = answerOf(lastState);
        return new ReactResult(task, answer, steps, traceOf(lastState));
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

    private static java.util.Optional<String> lastTextOf(AgentExecutor.State state) {
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

    public record ReactResult(
            String task,
            String answer,
            List<String> steps,
            List<String> agentMessages
    ) {}
}
