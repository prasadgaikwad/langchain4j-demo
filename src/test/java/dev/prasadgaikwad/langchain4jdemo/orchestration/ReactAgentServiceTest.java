package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReactAgentServiceTest {

    @Test
    void traceIncludesToolCallsAndResultsWithoutNulls() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("req-1")
                .name("calculator")
                .arguments("{\"a\": 15, \"b\": 340}")
                .build();

        List<ChatMessage> messages = List.of(
                UserMessage.from("What is 15% of 340?"),
                AiMessage.from(request),
                ToolExecutionResultMessage.from("req-1", "calculator", "51"),
                AiMessage.from("15% of 340 is 51."));

        AgentExecutor.State state = new AgentExecutor.State(Map.of("messages", messages));

        List<String> trace = ReactAgentService.traceOf(state);

        assertThat(trace).containsExactly(
                "tool_call: calculator({\"a\": 15, \"b\": 340})",
                "tool_result[calculator]: 51",
                "15% of 340 is 51.");
    }

    @Test
    void traceKeepsReasoningTextBeforeToolCall() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("req-2")
                .name("weather")
                .arguments("{\"city\": \"Tokyo\"}")
                .build();

        List<ChatMessage> messages = List.of(
                AiMessage.from("I should check the weather.", List.of(request)),
                ToolExecutionResultMessage.from("req-2", "weather", "22C"));

        AgentExecutor.State state = new AgentExecutor.State(Map.of("messages", messages));

        List<String> trace = ReactAgentService.traceOf(state);

        assertThat(trace).containsExactly(
                "I should check the weather.",
                "tool_call: weather({\"city\": \"Tokyo\"})",
                "tool_result[weather]: 22C");
    }

    @Test
    void answerPrefersFinalResponseOverLastAiText() {
        AgentExecutor.State state = new AgentExecutor.State(Map.of(
                "messages", List.of(AiMessage.from("intermediate reasoning")),
                "agent_response", "The final answer"));

        assertThat(ReactAgentService.answerOf(state)).isEqualTo("The final answer");
    }

    @Test
    void answerFallsBackToLastNonBlankAiText() {
        AgentExecutor.State state = new AgentExecutor.State(Map.of(
                "messages", List.of(
                        AiMessage.from("first"),
                        AiMessage.from("last real answer"))));

        assertThat(ReactAgentService.answerOf(state)).isEqualTo("last real answer");
    }

    @Test
    void answerReturnsNoResponseForNullState() {
        assertThat(ReactAgentService.answerOf(null)).isEqualTo("No response");
    }
}
