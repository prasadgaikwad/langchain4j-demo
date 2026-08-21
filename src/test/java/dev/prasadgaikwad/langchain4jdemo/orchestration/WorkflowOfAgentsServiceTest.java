package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowOfAgentsServiceTest {

    private static final String TECH_RESEARCH = "Technical research on AI agents.";
    private static final String TREND_RESEARCH = "Trending use cases for AI agents.";
    private static final String DRAFT = "AI agents are transforming software development.";
    private static final String SCORE_LOW = "0.5";
    private static final String SCORE_HIGH = "0.9";
    private static final String IMPROVED_DRAFT = "AI agents are revolutionizing modern software development workflows.";
    private static final String CATEGORY_TECHNICAL = "technical";
    private static final String TECH_FORMATTED = "# AI Agents\n\nAI agents are revolutionizing modern software development workflows.";

    @Test
    void workflowRunsAllAgentsAndReturnsFormattedResult() {
        ScriptedWorkflowChatModel chatModel = new ScriptedWorkflowChatModel(List.of(
                TECH_RESEARCH, TREND_RESEARCH,
                DRAFT,
                SCORE_LOW, IMPROVED_DRAFT,
                SCORE_HIGH,
                CATEGORY_TECHNICAL,
                TECH_FORMATTED));
        WorkflowOfAgentsService service = new WorkflowOfAgentsService(chatModel);

        WorkflowPipelineResult result = service.run("AI agents");

        assertThat(result.topic()).isEqualTo("AI agents");
        assertThat(result.research()).isNotEmpty();
        assertThat(result.draft()).isNotEmpty();
        assertThat(result.formatted()).isEqualTo(TECH_FORMATTED);
        assertThat(result.category()).isEqualTo(CATEGORY_TECHNICAL);
    }

    @Test
    void workflowLoopTerminatesWhenScoreExceedsThreshold() {
        ScriptedWorkflowChatModel chatModel = new ScriptedWorkflowChatModel(List.of(
                TECH_RESEARCH, TREND_RESEARCH,
                DRAFT,
                SCORE_HIGH,
                CATEGORY_TECHNICAL,
                TECH_FORMATTED));
        WorkflowOfAgentsService service = new WorkflowOfAgentsService(chatModel);

        WorkflowPipelineResult result = service.run("Quick topic");

        assertThat(result.category()).isEqualTo(CATEGORY_TECHNICAL);
        // 2 parallel + 1 draft + 1 score (exits immediately) + 1 category + 1 format = 6
        assertThat(chatModel.calls).isEqualTo(6);
    }

    @Test
    void workflowLoopRunsMultipleIterations() {
        ScriptedWorkflowChatModel chatModel = new ScriptedWorkflowChatModel(List.of(
                TECH_RESEARCH, TREND_RESEARCH,
                DRAFT,
                SCORE_LOW, IMPROVED_DRAFT,
                SCORE_LOW, IMPROVED_DRAFT,
                SCORE_HIGH,
                CATEGORY_TECHNICAL,
                TECH_FORMATTED));
        WorkflowOfAgentsService service = new WorkflowOfAgentsService(chatModel);

        WorkflowPipelineResult result = service.run("Complex topic");

        assertThat(result.category()).isEqualTo(CATEGORY_TECHNICAL);
    }

    @Test
    void workflowSelectsConditionalBranch() {
        ScriptedWorkflowChatModel chatModel = new ScriptedWorkflowChatModel(List.of(
                TECH_RESEARCH, TREND_RESEARCH,
                DRAFT,
                SCORE_HIGH,
                "general",
                "# General Post\n\nAI agents are useful for everyone."));
        WorkflowOfAgentsService service = new WorkflowOfAgentsService(chatModel);

        WorkflowPipelineResult result = service.run("Cooking tips");

        assertThat(result.category()).isEqualTo("general");
        assertThat(result.formatted()).contains("General Post");
    }

    private static final class ScriptedWorkflowChatModel implements ChatModel {

        private final List<String> responses;
        private final List<ChatRequest> requests = new ArrayList<>();
        int calls;

        ScriptedWorkflowChatModel(List<String> responses) {
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
