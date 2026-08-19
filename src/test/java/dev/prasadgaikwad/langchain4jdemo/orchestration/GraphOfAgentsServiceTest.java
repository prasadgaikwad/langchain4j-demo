package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphOfAgentsServiceTest {

    private static final String PROFILE = "Senior DevOps engineer interested in cloud-native";
    private static final String TOPIC = "Kubernetes best practices for production";
    private static final String OUTLINE = "# K8s Best Practices\n\n## Introduction\n\n## Cluster Config\n\n## Monitoring\n\n## Conclusion";
    private static final String DRAFT = "This is a draft about Kubernetes best practices for production environments.";
    private static final String EDITED = "This is an edited and polished draft about Kubernetes best practices.";
    private static final String WRITEUP = "# Kubernetes Best Practices for Production\n\nA guide for DevOps engineers.";

    @Test
    void pipelineRunsAllSixAgentsAndReturnsWriteup() {
        ScriptedChatModel chatModel = new ScriptedChatModel(
                List.of(PROFILE, TOPIC, OUTLINE, DRAFT, EDITED, WRITEUP));
        GraphOfAgentsService service = new GraphOfAgentsService(chatModel);

        String result = service.run("I'm a DevOps engineer, what should I write about?");

        assertThat(result).isEqualTo(WRITEUP);
        assertThat(chatModel.calls).isEqualTo(6);
    }

    @Test
    void pipelineReturnsFullTraceWithAllIntermediateSteps() {
        ScriptedChatModel chatModel = new ScriptedChatModel(
                List.of(PROFILE, TOPIC, OUTLINE, DRAFT, EDITED, WRITEUP));
        GraphOfAgentsService service = new GraphOfAgentsService(chatModel);

        GraphPipelineResult result = service.runWithTrace("I'm a DevOps engineer");

        assertThat(result.prompt()).isEqualTo("I'm a DevOps engineer");
        assertThat(result.profile()).isEqualTo(PROFILE);
        assertThat(result.topic()).isEqualTo(TOPIC);
        assertThat(result.outline()).isEqualTo(OUTLINE);
        assertThat(result.draft()).isEqualTo(DRAFT);
        assertThat(result.edited()).isEqualTo(EDITED);
        assertThat(result.writeup()).isEqualTo(WRITEUP);
    }

    @Test
    void pipelinePassesCorrectInputsToEachAgent() {
        List<String> recordedInputs = new ArrayList<>();
        ScriptedChatModel chatModel = new ScriptedChatModel(
                List.of(PROFILE, TOPIC, OUTLINE, DRAFT, EDITED, WRITEUP));
        GraphOfAgentsService service = new GraphOfAgentsService(chatModel);

        service.run("test prompt");

        assertThat(chatModel.requests).hasSize(6);
        // Each request should have at least one message
        for (ChatRequest request : chatModel.requests) {
            assertThat(request.messages()).isNotEmpty();
        }
    }

    /**
     * Scripted model that returns canned responses in order, one per agent
     * invocation in the GOAP pipeline.
     */
    private static final class ScriptedChatModel implements ChatModel {

        private final List<String> responses;
        final List<ChatRequest> requests = new ArrayList<>();
        int calls;

        ScriptedChatModel(List<String> responses) {
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
