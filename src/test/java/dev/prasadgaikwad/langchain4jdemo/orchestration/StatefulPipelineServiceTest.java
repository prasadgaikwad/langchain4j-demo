package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #249: repeating a sessionId must not freeze the
 * answer at the first turn's text. The persisted {@code agent_response} value
 * short-circuits executeTool to END on later turns, so tools executed without
 * a synthesizing agent step.
 */
class StatefulPipelineServiceTest {

    /** Turn 1 model: answers plain text immediately (sets agent_response). */
    static final class GreetingThenToolModel implements ChatModel {
        final List<List<ChatMessage>> receivedRequests = new ArrayList<>();

        @Override
        public ChatResponse doChat(ChatRequest request) {
            receivedRequests.add(request.messages());
            boolean lastIsToolResult = request.messages().stream()
                    .reduce((a, b) -> b)
                    .map(m -> m.type() == ChatMessageType.TOOL_EXECUTION_RESULT)
                    .orElse(false);
            if (receivedRequests.size() == 1) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("Hello! How can I assist you today?"))
                        .build();
            }
            if (!lastIsToolResult) {
                var req = ToolExecutionRequest.builder()
                        .name("calculate")
                        .arguments("{\"expression\": \"2 + 3\"}")
                        .id("call_" + receivedRequests.size())
                        .build();
                return ChatResponse.builder().aiMessage(AiMessage.from(req)).build();
            }
            return ChatResponse.builder().aiMessage(AiMessage.from("The sum is 5.")).build();
        }
    }

    private StatefulPipelineService newService(GreetingThenToolModel model) throws Exception {
        return new StatefulPipelineService(
                new AgentGraphFactory(model,
                        new CalculatorTool(),
                        new DocumentSearchTool(null),
                        new WeatherTool(),
                        new EmbeddingStoreStatsTool(null)),
                new BoundedMemorySaver(10_000));
    }

    @Test
    void secondTurnWithToolCallSynthesizesInsteadOfReturningStaleAnswer() throws Exception {
        var model = new GreetingThenToolModel();
        StatefulPipelineService service = newService(model);

        var first = service.run("sess", "hi");
        assertEquals("Hello! How can I assist you today?", first.answer());

        var second = service.run("sess", "what is 2 + 3?");

        assertTrue(second.steps().stream().filter("agent"::equals).count() >= 2,
                "second turn must end with a synthesizing agent step; steps=" + second.steps());
        assertEquals("The sum is 5.", second.answer(),
                "answer must come from THIS turn's tool round, not turn 1");
    }

    @Test
    void secondTurnStillSeesFirstTurnConversation() throws Exception {
        var model = new GreetingThenToolModel();
        StatefulPipelineService service = newService(model);

        service.run("sess2", "hi");
        service.run("sess2", "what is 2 + 3?");

        // The model call for the second turn must contain the first turn's
        // user message and answer as context.
        List<ChatMessage> secondCall = model.receivedRequests.get(1);
        String allText = secondCall.stream()
                .map(m -> m instanceof AiMessage ai ? ai.text() : m.toString())
                .reduce("", (a, b) -> a + " " + b);
        assertTrue(allText.contains("Hello! How can I assist you today?"),
                "second turn prompt should include turn-1 context");
    }

    @Test
    void historyReflectsCurrentRunNotAllPastRuns() throws Exception {
        var model = new GreetingThenToolModel();
        StatefulPipelineService service = newService(model);

        service.run("sess3", "hi");
        var second = service.run("sess3", "what is 2 + 3?");

        assertTrue(second.checkpointCount() <= 5,
                "history should cover this run only, not every run on the session"
                        + " (was " + second.checkpointCount() + ")");
        assertFalse(second.history().isEmpty());
    }

    @Test
    void transcriptSeededIntoEachRunIsWindowedToTheConfiguredBudget() throws Exception {
        PlainAnswerModel model = new PlainAnswerModel();
        // Window of 4 lets the first turn (2 messages) seed in full, then keeps
        // only the most recent 4 as the session grows.
        StatefulPipelineService service = new StatefulPipelineService(
                new AgentGraphFactory(model,
                        new CalculatorTool(),
                        new DocumentSearchTool(null),
                        new WeatherTool(),
                        new EmbeddingStoreStatsTool(null)),
                new BoundedMemorySaver(10_000),
                4);

        for (int i = 0; i < 6; i++) {
            service.run("sess-window", "turn " + i);
        }

        // The last run's prompt must stay bounded by the window, not grow with
        // the whole session. The window caps the seeded prior messages, so the
        // prompt stabilizes at a small constant instead of growing without bound
        // (which would have reached 12+ messages across 6 turns).
        java.util.List<Integer> sizes = model.receivedRequests.stream().map(List::size).toList();
        assertTrue(sizes.stream().max(Integer::compareTo).orElse(0) <= 6,
                "seeded transcript must be windowed; sizes=" + sizes);
        // The window must actually engage: once the session exceeds the window,
        // the prompt must stop growing (last sizes equal, not increasing).
        int last = sizes.get(sizes.size() - 1);
        assertTrue(last == sizes.get(sizes.size() - 2),
                "window should bound prompt growth; sizes=" + sizes);
        // Expect at least one turn where the window actually dropped old context
        // (session had 6+ turns but each prompt stayed within the window).
        assertTrue(model.receivedRequests.size() >= 6);
    }

    /** Always replies with a plain text answer so each turn is self-contained. */
    private static final class PlainAnswerModel implements ChatModel {
        final List<List<ChatMessage>> receivedRequests = new ArrayList<>();

        @Override
        public ChatResponse doChat(ChatRequest request) {
            receivedRequests.add(new ArrayList<>(request.messages()));
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("ok"))
                    .build();
        }
    }
}
