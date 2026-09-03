package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.ai.DynamicAgent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicToolProviderTest {

    @Test
    void weatherToolIsExposedWhenTheTaskMentionsWeather() {
        ToolLoopChatModel model = new ToolLoopChatModel();
        DynamicAgent agent = buildAgent(model);

        String answer = agent.execute("mem-weather", "What's the weather in London?");

        assertThat(answer).contains("15.0");
        assertThat(model.toolNamesOfCall(0)).contains("getWeather", "calculate");
    }

    @Test
    void weatherToolIsHiddenForNonWeatherTasks() {
        ToolLoopChatModel model = new ToolLoopChatModel();
        DynamicAgent agent = buildAgent(model);

        agent.execute("mem-other", "What is 2 + 3?");

        assertThat(model.toolNamesOfCall(0)).doesNotContain("getWeather").contains("calculate");
    }

    @Test
    void executesTheWeatherToolEndToEnd() {
        ToolLoopChatModel model = new ToolLoopChatModel();
        DynamicAgent agent = buildAgent(model);

        String answer = agent.execute("mem-weather", "What's the weather in London?");

        assertThat(model.toolNamesOfCall(0)).contains("getWeather");
        assertThat(model.toolResultsOfCall(1)).contains("It is 15.0 degrees celsius in London.");
        assertThat(answer).contains("London");
    }

    private static DynamicAgent buildAgent(ChatModel model) {
        CalculatorTool calculatorTool = new CalculatorTool();
        WeatherTool weatherTool = new WeatherTool();
        NoteTool noteTool = new NoteTool(100);
        DynamicToolProvider provider = new DynamicToolProvider(calculatorTool, weatherTool, noteTool);
        return AiServices.builder(DynamicAgent.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .toolProvider(provider)
                .build();
    }

    /**
     * Returns a tool call on the first model call, then a plain answer, so the
     * agent loop (call model, execute tool, call model again) can run offline.
     */
    private static class ToolLoopChatModel implements ChatModel {

        private final List<String> toolNamesPerCall = new ArrayList<>();
        private final List<String> toolResultsPerCall = new ArrayList<>();
        private int calls = 0;

        @Override
        public ChatResponse doChat(ChatRequest request) {
            List<String> toolNames = request.toolSpecifications() == null ? List.of()
                    : request.toolSpecifications().stream().map(ToolSpecification::name).toList();
            toolNamesPerCall.add(String.join(",", toolNames));

            String toolResult = request.messages().stream()
                    .filter(message -> message.type() == dev.langchain4j.data.message.ChatMessageType.TOOL_EXECUTION_RESULT)
                    .map(message -> ((dev.langchain4j.data.message.ToolExecutionResultMessage) message).text())
                    .findFirst()
                    .orElse("");
            toolResultsPerCall.add(toolResult);

            calls++;
            if (calls == 1) {
                boolean hasWeatherTool = request.toolSpecifications().stream()
                        .anyMatch(specification -> specification.name().equals("getWeather"));
                String toolName = hasWeatherTool ? "getWeather" : "calculate";
                String arguments = hasWeatherTool
                        ? "{\"city\":\"London\",\"unit\":\"CELSIUS\"}"
                        : "{\"expression\":\"2 + 3\"}";
                ToolExecutionRequest toolCall = ToolExecutionRequest.builder()
                        .name(toolName)
                        .arguments(arguments)
                        .id("call_1")
                        .build();
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(List.of(toolCall)))
                        .build();
            }
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("London is 15.0 degrees celsius today."))
                    .build();
        }

        List<String> toolNamesOfCall(int callIndex) {
            return List.of(toolNamesPerCall.get(callIndex).split(","));
        }

        String toolResultsOfCall(int callIndex) {
            return toolResultsPerCall.get(callIndex);
        }
    }
}
