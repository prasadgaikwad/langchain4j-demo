package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #247: HITL must gate ONLY real tool proposals,
 * approval must resume execution of the approved tools, rejection must never
 * execute them, and tool-free runs must complete without any prompt.
 */
class HumanInTheLoopServiceTest {

    /** Always answers plain text — no tool calls ever proposed. */
    static final class TextOnlyModel implements ChatModel {
        private final String answer;

        TextOnlyModel(String answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            return ChatResponse.builder().aiMessage(AiMessage.from(answer)).build();
        }
    }

    /**
     * Scripted model: every turn that does not follow a tool result proposes
     * {@code calculate(2 + 3)}; the turn after a tool result answers from it.
     */
    static final class SingleProposalModel implements ChatModel {
        int modelCalls = 0;

        @Override
        public ChatResponse doChat(ChatRequest request) {
            modelCalls++;
            boolean lastIsToolResult = request.messages().stream()
                    .reduce((a, b) -> b)
                    .map(m -> m.type() == ChatMessageType.TOOL_EXECUTION_RESULT)
                    .orElse(false);
            if (!lastIsToolResult) {
                var req = ToolExecutionRequest.builder()
                        .name("calculate")
                        .arguments("{\"expression\": \"2 + 3\"}")
                        .id("call_1")
                        .build();
                return ChatResponse.builder().aiMessage(AiMessage.from(req)).build();
            }
            return ChatResponse.builder().aiMessage(AiMessage.from("The sum is 5.")).build();
        }
    }

    /**
     * Scripted model proposing a tool TWICE before answering — verifies each
     * approval advances exactly one round and re-parking is detected again.
     */
    static final class TwoRoundProposalModel implements ChatModel {
        int modelCalls = 0;

        @Override
        public ChatResponse doChat(ChatRequest request) {
            boolean lastIsToolResult = request.messages().stream()
                    .reduce((a, b) -> b)
                    .map(m -> m.type() == ChatMessageType.TOOL_EXECUTION_RESULT)
                    .orElse(false);
            modelCalls++;
            // Round 1: no tool result yet -> propose. Round 2: first result
            // seen -> propose once more. Round 3: answer from the results.
            if (!lastIsToolResult || modelCalls == 2) {
                var req = ToolExecutionRequest.builder()
                        .name("calculate")
                        .arguments("{\"expression\": \"7 * 6\"}")
                        .id("call_" + modelCalls)
                        .build();
                return ChatResponse.builder().aiMessage(AiMessage.from(req)).build();
            }
            return ChatResponse.builder().aiMessage(AiMessage.from("Forty-two.")).build();
        }
    }

    private HumanInTheLoopService newService(ChatModel model) throws Exception {
        return new HumanInTheLoopService(
                new AgentGraphFactory(model,
                        new CalculatorTool(),
                        new DocumentSearchTool(null),
                        new WeatherTool(),
                        new EmbeddingStoreStatsTool(null)),
                new BoundedMemorySaver(10_000));
    }

    @Test
    void toolFreeRunCompletesWithoutApprovalPrompt() throws Exception {
        HumanInTheLoopService service = newService(
                new TextOnlyModel("Records are concise immutable data carriers."));

        var result = service.start("s1", "write a small paragraph about java record");

        assertFalse(result.awaitingApproval(),
                "tool-free run must not enter the approval loop");
        assertTrue(result.proposedAction().isEmpty());
        assertEquals(List.of("agent", "action"), result.steps(),
                "tool-free park is silently driven through action to END");
        assertTrue(result.answer().contains("Records are concise"));
    }

    @Test
    void toolProposalGatesThenApprovalExecutesToolExactlyOnce() throws Exception {
        var model = new SingleProposalModel();
        HumanInTheLoopService service = newService(model);

        var started = service.start("s2", "what is 2 + 3?");
        assertEquals(1, model.modelCalls,
                "start must make exactly one LLM call, not loop");
        assertTrue(started.awaitingApproval());
        assertTrue(started.proposedAction().contains("tool_call: calculate"),
                "proposal should render the actual tool request");
        assertTrue(started.proposedAction().contains("2 + 3"));

        var resumed = service.resume("s2", true, "");
        assertFalse(resumed.awaitingApproval());
        assertEquals(2, model.modelCalls,
                "resume must continue at the action node, NOT re-run the agent");
        assertEquals("The sum is 5.", resumed.answer());
        assertTrue(resumed.steps().contains("action"),
                "approved tools must actually execute");
    }

    @Test
    void rejectionTerminatesSessionWithoutExecutingTools() throws Exception {
        var model = new SingleProposalModel();
        HumanInTheLoopService service = newService(model);

        var started = service.start("s3", "what is 2 + 3?");
        assertTrue(started.awaitingApproval());
        int callsBeforeRejection = model.modelCalls;

        var rejected = service.resume("s3", false, "do not compute this");

        assertFalse(rejected.awaitingApproval());
        assertEquals(callsBeforeRejection, model.modelCalls,
                "rejected tools must never execute and no extra LLM call may happen");
    }

    @Test
    void secondProposalAfterApprovalIsDetectedAgain() throws Exception {
        var model = new TwoRoundProposalModel();
        HumanInTheLoopService service = newService(model);

        var first = service.start("s4", "what is 7 * 6?");
        assertTrue(first.awaitingApproval());

        var second = service.resume("s4", true, "");
        assertTrue(second.awaitingApproval(),
                "a fresh proposal after approved execution must re-park");

        var third = service.resume("s4", true, "");
        assertFalse(third.awaitingApproval());
        assertEquals("Forty-two.", third.answer());
    }

    @Test
    void getPendingActionReflectsGateState() throws Exception {
        var model = new SingleProposalModel();
        HumanInTheLoopService service = newService(model);

        assertTrue(service.getPendingAction("unknown").isEmpty());

        var started = service.start("s5", "what is 2 + 3?");
        assertTrue(service.getPendingAction("s5").isPresent());
        assertEquals(started.proposedAction(),
                service.getPendingAction("s5").orElseThrow().proposedAction());
    }
}
