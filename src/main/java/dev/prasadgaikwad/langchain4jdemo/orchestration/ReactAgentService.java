package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReactAgentService {

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    public ReactAgentService(ChatModel chatModel,
                             CalculatorTool calculatorTool,
                             DocumentSearchTool documentSearchTool,
                             WeatherTool weatherTool,
                             EmbeddingStoreStatsTool storeStatsTool) throws GraphStateException {
        StateGraph<AgentExecutor.State> graph = AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(calculatorTool, documentSearchTool, weatherTool, storeStatsTool)
                .build();

        this.compiledGraph = graph.compile();
    }

    public ReactResult run(String task) {
        List<String> steps = new ArrayList<>();

        var generator = compiledGraph.stream(Map.of("messages", UserMessage.from(task)));
        for (var item : generator) {
            String nodeName = item.node();
            if (!"__START__".equals(nodeName) && !"__END__".equals(nodeName)) {
                steps.add(nodeName);
            }
        }

        Optional<AgentExecutor.State> finalState = compiledGraph.invoke(
                Map.of("messages", UserMessage.from(task)));

        String answer = finalState
                .flatMap(AgentExecutor.State::finalResponse)
                .orElse("No response");

        List<String> allMessages = finalState
                .map(s -> s.messages().stream()
                        .filter(m -> m instanceof AiMessage)
                        .map(m -> ((AiMessage) m).text())
                        .toList())
                .orElse(List.of());

        return new ReactResult(task, answer, steps, allMessages);
    }

    public record ReactResult(
            String task,
            String answer,
            List<String> steps,
            List<String> agentMessages
    ) {}
}
