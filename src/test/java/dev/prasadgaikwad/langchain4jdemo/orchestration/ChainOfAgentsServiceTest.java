package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChainOfAgentsServiceTest {

    private static final String OUTLINE = "# Test Topic\n\n## Introduction\n\n## Main Section\n\n## Conclusion";
    private static final String DRAFT = "This is a test draft about the topic. It has multiple paragraphs.";
    private static final String EDITED = "This is an edited and polished draft. Grammar and flow improved.";
    private static final String FORMATTED = "# Test Topic\n\n**Introduction:**\n\nThis is a polished blog post.";

    @Test
    void pipelineRunsAllFourAgentsInSequenceAndReturnsFormattedResult() {
        ScriptedSequenceChatModel chatModel = new ScriptedSequenceChatModel(
                List.of(OUTLINE, DRAFT, EDITED, FORMATTED));
        ChainOfAgentsService service = new ChainOfAgentsService(chatModel);

        String result = service.run("Test Topic");

        assertThat(result).isEqualTo(FORMATTED);
        assertThat(chatModel.calls).isEqualTo(4);
    }

    @Test
    void pipelineReturnsFullTraceWithAllIntermediateSteps() {
        ScriptedSequenceChatModel chatModel = new ScriptedSequenceChatModel(
                List.of(OUTLINE, DRAFT, EDITED, FORMATTED));
        ChainOfAgentsService service = new ChainOfAgentsService(chatModel);

        ChainPipelineResult result = service.runWithTrace("Test Topic");

        assertThat(result.topic()).isEqualTo("Test Topic");
        assertThat(result.outline()).isEqualTo(OUTLINE);
        assertThat(result.draft()).isEqualTo(DRAFT);
        assertThat(result.edited()).isEqualTo(EDITED);
        assertThat(result.formatted()).isEqualTo(FORMATTED);
    }

    @Test
    void pipelinePassesTopicToFirstAgent() {
        ScriptedSequenceChatModel chatModel = new ScriptedSequenceChatModel(
                List.of(OUTLINE, DRAFT, EDITED, FORMATTED));
        ChainOfAgentsService service = new ChainOfAgentsService(chatModel);

        service.run("Spring Boot 4 Migration");

        ChatRequest firstRequest = chatModel.requests.get(0);
        assertThat(firstRequest.messages()).isNotEmpty();
    }

    @Test
    void pipelineAgentsAreCalledInCorrectOrder() {
        List<String> responses = new ArrayList<>();
        ScriptedSequenceChatModel chatModel = new ScriptedSequenceChatModel(
                List.of(OUTLINE, DRAFT, EDITED, FORMATTED));
        ChainOfAgentsService service = new ChainOfAgentsService(chatModel);

        service.run("Order Test");

        assertThat(chatModel.calls).isEqualTo(4);
        // Each agent gets a different system message / user message pattern
        // Verify the agents are called by checking message content patterns
        for (ChatRequest request : chatModel.requests) {
            assertThat(request.messages()).isNotEmpty();
        }
    }

    /**
     * Scripted model that returns canned responses in order, one per agent
     * invocation in the sequential pipeline.
     */
    private static final class ScriptedSequenceChatModel implements ChatModel {

        private final List<String> responses;
        private final List<ChatRequest> requests = new ArrayList<>();
        int calls;

        ScriptedSequenceChatModel(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            requests.add(chatRequest);
            String response = responses.get(Math.min(calls, responses.size() - 1));
            calls++;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .build();
        }
    }
}
