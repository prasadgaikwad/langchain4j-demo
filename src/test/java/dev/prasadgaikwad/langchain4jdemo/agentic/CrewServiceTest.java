package dev.prasadgaikwad.langchain4jdemo.agentic;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrewServiceTest {

    private CrewService crew(ChatModel chatModel) {
        SemanticSearchService searchService = new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
        return new CrewService(
                chatModel,
                new ChatMemoryRegistry(),
                new CalculatorTool(),
                new WeatherTool(),
                new DocumentSearchTool(searchService));
    }

    @Test
    void crewRunsTaskThroughTheSupervisorAndReturnsTheDelegatedResult() {
        ScriptedSupervisorChatModel chatModel = new ScriptedSupervisorChatModel("Weather looks sunny.");
        CrewService crew = crew(chatModel);

        String result = crew.run("What is the weather?");

        assertThat(result).isEqualTo("Weather looks sunny.");
        assertThat(chatModel.calls).isGreaterThanOrEqualTo(3);
        assertThat(chatModel.requests.get(0).messages()).isNotEmpty();
    }

    @Test
    void subAgentExposesItsToolToTheCrewModel() {
        ScriptedSupervisorChatModel chatModel = new ScriptedSupervisorChatModel("Weather looks sunny.");
        CrewService crew = crew(chatModel);

        crew.run("What is the weather?");

        ChatRequest subAgentRequest = chatModel.requests.get(1);
        assertThat(subAgentRequest.toolSpecifications()).isNotEmpty();
        assertThat(subAgentRequest.toolSpecifications())
                .extracting(spec -> spec.name())
                .contains("getWeather");
    }

    /**
     * Scripted model for the supervisor protocol: the first call asks the
     * supervisor to delegate to the Weather sub-agent (JSON
     * {@code AgentInvocation}), the second answers plainly (the sub-agent's
     * reply), and any later call tells the supervisor to wrap up and return the
     * delegated result.
     */
    private static final class ScriptedSupervisorChatModel implements ChatModel {

        private final String subAgentResult;
        private final List<ChatRequest> requests = new ArrayList<>();
        int calls;

        ScriptedSupervisorChatModel(String subAgentResult) {
            this.subAgentResult = subAgentResult;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            requests.add(chatRequest);
            calls++;
            if (calls == 1) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("{\"agentName\": \"Weather\", \"arguments\": {\"task\": \"weather report\"}}"))
                        .build();
            }
            if (calls == 2) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(subAgentResult))
                        .build();
            }
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("{\"agentName\": \"done\"}"))
                    .build();
        }
    }
}
