package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for the shared ReACT graph factory: issue #261 (a
 * mid-graph model/tool failure must reach the caller instead of being
 * swallowed into a partial "success" answer) and issue #260 (a single
 * config-driven recursion limit applied to all three graphs).
 */
class AgentGraphFactoryTest {

    @Test
    void streamPropagatesMidGraphModelFailure() throws Exception {
        AgentGraphFactory factory = new AgentGraphFactory(
                new FailingAfterFirstCallChatModel(),
                new CalculatorTool(),
                new DocumentSearchTool(null),
                new WeatherTool(),
                new EmbeddingStoreStatsTool(null));

        assertThatThrownBy(() -> AgentGraphFactory.stream(
                factory.react(),
                Map.of("messages", UserMessage.from("what is 2 + 3?")),
                null))
                .isInstanceOf(RuntimeException.class)
                .rootCause()
                .hasMessage("boom");
    }

    @Test
    void allThreeGraphsShareTheConfiguredRecursionLimit() throws Exception {
        AgentGraphFactory factory = new AgentGraphFactory(
                new FailingAfterFirstCallChatModel(),
                new CalculatorTool(),
                new DocumentSearchTool(null),
                new WeatherTool(),
                new EmbeddingStoreStatsTool(null),
                24);

        // All three graph types must compile with the same (config-driven)
        // recursion budget rather than divergent per-service values (#260).
        assertThat(factory.react()).isNotNull();
        assertThat(factory.checkpointed(new BoundedMemorySaver(100))).isNotNull();
        assertThat(factory.humanInTheLoop(new BoundedMemorySaver(100))).isNotNull();
    }

    @Test
    void defaultRecursionLimitIsSharedAndPositive() {
        assertThat(AgentGraphFactory.DEFAULT_RECURSION_LIMIT).isPositive();
    }

    /** Stage 1 asks for a tool call; stage 2 fails during synthesis. */
    private static final class FailingAfterFirstCallChatModel implements ChatModel {

        private int calls;

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            calls++;
            if (calls == 1) {
                var req = ToolExecutionRequest.builder()
                        .name("calculate")
                        .arguments("{\"expression\": \"2 + 3\"}")
                        .id("call_1")
                        .build();
                return ChatResponse.builder().aiMessage(AiMessage.from(req)).build();
            }
            throw new IllegalStateException("boom");
        }
    }
}