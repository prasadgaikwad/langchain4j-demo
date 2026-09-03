package dev.prasadgaikwad.langchain4jdemo.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelRegistryTest {

    private ModelRegistry registryWithFakes() {
        Map<String, dev.langchain4j.model.chat.ChatModel> models = new LinkedHashMap<>();
        models.put("openai:gpt-4o-mini", new FakeChatModel("openai answer"));
        models.put("anthropic:claude-haiku-4-5-20251001", new FakeChatModel("anthropic answer"));
        models.put("ollama:llama3.2", new FakeChatModel("ollama answer"));
        return new ModelRegistry(LlmProvider.OPENAI, "gpt-4o-mini", models);
    }

    @Test
    void usesTheInitialSelectionUntilSwitched() {
        ModelRegistry registry = registryWithFakes();

        assertThat(registry.currentProvider()).isEqualTo(LlmProvider.OPENAI);
        assertThat(registry.currentModelName()).isEqualTo("gpt-4o-mini");
        assertThat(registry.currentLabel()).isEqualTo("openai:gpt-4o-mini");
        assertThat(registry.currentChatModel()).isInstanceOf(FakeChatModel.class);
    }

    @Test
    void switchesProviderWithItsDefaultModel() {
        ModelRegistry registry = registryWithFakes();

        registry.setModel("anthropic");

        assertThat(registry.currentProvider()).isEqualTo(LlmProvider.ANTHROPIC);
        assertThat(registry.currentLabel()).isEqualTo("anthropic:claude-haiku-4-5-20251001");
        assertThat(registry.currentChatModel()).isInstanceOf(FakeChatModel.class);
    }

    @Test
    void switchesProviderAndModelByColonSpec() {
        ModelRegistry registry = registryWithFakes();

        registry.setModel("ollama:llama3.2");

        assertThat(registry.currentLabel()).isEqualTo("ollama:llama3.2");
    }

    @Test
    void rejectsUnknownProviders() {
        ModelRegistry registry = registryWithFakes();

        assertThatThrownBy(() -> registry.setModel("nope:some-model"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown provider");
    }

    @Test
    void delegatesChatCallsToTheCurrentModel() {
        ModelRegistry registry = registryWithFakes();

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from("hello"))
                .build();
        ChatResponse response = registry.doChat(request);

        assertThat(response.aiMessage()).isEqualTo(AiMessage.from("openai answer"));
        assertThat(((FakeChatModel) registry.currentChatModel()).lastRequest()).isSameAs(request);
    }

    @Test
    void comparisonSetIsFixedToTheRegisteredModels() {
        ModelRegistry registry = registryWithFakes();

        assertThat(registry.availableModels()).containsExactly(
                "anthropic:claude-haiku-4-5-20251001", "ollama:llama3.2", "openai:gpt-4o-mini");
    }

    @Test
    void buildsRealModelsLazilyWithoutAnApiKey() {
        ModelRegistry registry = new ModelRegistry("openai", "gpt-4o-mini",
                "claude-haiku-4-5-20251001", "gemini-2.5-flash", "llama3.2",
                "http://localhost:11434");

        registry.setModel("ollama");

        assertThat(registry.currentChatModel()).isNotNull();
    }

    @Test
    void delegatesModelCapabilitiesToTheCurrentModel() {
        ProviderAwareFakeChatModel model = new ProviderAwareFakeChatModel("answer");
        ModelRegistry registry = new ModelRegistry(LlmProvider.OPENAI, "gpt-4o-mini",
                Map.of("openai:gpt-4o-mini", model));

        assertThat(registry.defaultRequestParameters()).isInstanceOf(OpenAiChatRequestParameters.class);
        assertThat(registry.supportedCapabilities()).containsExactly(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
        assertThat(registry.provider()).isEqualTo(ModelProvider.GOOGLE_AI_GEMINI);
    }

    /**
     * Regression test for #218: {@code ChatModel.chat(ChatRequest)} merges
     * {@code defaultRequestParameters()} into the request it sends. The registry
     * must delegate that to the current model, otherwise the request carries
     * {@code DefaultChatRequestParameters} and {@code OpenAiChatModel.doChat}
     * throws a {@code ClassCastException}.
     */
    @Test
    void chatThroughRegistrySendsProviderSpecificRequestParameters() {
        ProviderAwareFakeChatModel model = new ProviderAwareFakeChatModel("answer");
        ModelRegistry registry = new ModelRegistry(LlmProvider.OPENAI, "gpt-4o-mini",
                Map.of("openai:gpt-4o-mini", model));

        registry.chat(ChatRequest.builder().messages(UserMessage.from("hello")).build());

        assertThat(model.lastRequest().parameters()).isInstanceOf(OpenAiChatRequestParameters.class);
    }

    /**
     * Regression test for #270: a build performed while the provider has no API
     * key must NOT be cached, otherwise a transient misconfiguration poisons the
     * registry for the life of the process and the key is never re-read. Once a
     * key is present the (now working) model should be cached.
     */
    @Test
    void doesNotCacheABuildMadeWhileTheApiKeyIsMissing() {
        KeyControlledRegistry registry = new KeyControlledRegistry(false);

        registry.currentChatModel();
        registry.currentChatModel();

        assertThat(registry.buildCount()).as("no-key builds are not cached").isEqualTo(2);

        registry.setApiKeyPresent(true);
        registry.currentChatModel();
        registry.currentChatModel();

        assertThat(registry.buildCount()).as("once the key is present the model is cached").isEqualTo(3);
    }

    /**
     * {@link ModelRegistry} whose API key availability is faked and whose model
     * builds are counted, so the caching decision (issue #270) is observable
     * without real env vars or network calls.
     */
    private static final class KeyControlledRegistry extends ModelRegistry {

        private boolean apiKeyPresent;
        private int buildCount;

        KeyControlledRegistry(boolean apiKeyPresent) {
            super("openai", "gpt-4o-mini", "claude-haiku-4-5-20251001",
                    "gemini-2.5-flash", "llama3.2", "http://localhost:11434");
            this.apiKeyPresent = apiKeyPresent;
        }

        void setApiKeyPresent(boolean apiKeyPresent) {
            this.apiKeyPresent = apiKeyPresent;
        }

        int buildCount() {
            return buildCount;
        }

        @Override
        protected String apiKeyFor(LlmProvider provider) {
            return provider == LlmProvider.OPENAI && apiKeyPresent ? "fake-key" : null;
        }

        @Override
        protected ChatModel buildChatModel(LlmProvider provider, String modelName) {
            buildCount++;
            return new FakeChatModel("fake answer");
        }
    }

    /**
     * Fake whose provider-specific capabilities prove the registry delegates to
     * the current model instead of using the {@code ChatModel} interface
     * defaults ({@code DefaultChatRequestParameters}, empty capabilities,
     * {@code ModelProvider.OTHER}).
     */
    private static final class ProviderAwareFakeChatModel extends FakeChatModel {

        ProviderAwareFakeChatModel(String responseText) {
            super(responseText);
        }

        @Override
        public ChatRequestParameters defaultRequestParameters() {
            return OpenAiChatRequestParameters.builder().build();
        }

        @Override
        public Set<Capability> supportedCapabilities() {
            return Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
        }

        @Override
        public ModelProvider provider() {
            return ModelProvider.GOOGLE_AI_GEMINI;
        }
    }
}
