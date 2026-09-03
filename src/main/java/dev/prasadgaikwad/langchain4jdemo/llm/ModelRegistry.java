package dev.prasadgaikwad.langchain4jdemo.llm;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of chat models that lets the demo switch provider and model at
 * runtime. It implements {@link ChatModel} and is the bean injected everywhere
 * the app needs a chat model, so switching the selection switches every AI
 * service (assistant, RAG, agent, judge) at once.
 * <p>
 * Models are built lazily on first use and cached per {@code provider:model},
 * so starting the app never requires an API key for every provider. The
 * comparison set exposed by {@link #availableModels()} only contains providers
 * whose key is present in the environment (plus Ollama, which needs none).
 */
@Service
public class ModelRegistry implements ChatModel {

    private final Map<String, ChatModel> models = new ConcurrentHashMap<>();
    private final Map<LlmProvider, String> configuredModelNames = new LinkedHashMap<>();
    private final String ollamaBaseUrl;
    private final List<String> testAvailableModels;

    /**
     * Immutable snapshot of the current selection. Read as a single value so a
     * request never observes a torn {@code provider/model} pair while another
     * thread calls {@link #setModel} (issue #253). Marked volatile for safe
     * publication.
     */
    private volatile Selection selection;

    /** Immutable {@code (provider, modelName)} pair. */
    private record Selection(LlmProvider provider, String modelName) {}

    @Autowired
    public ModelRegistry(@Value("${app.chat.provider:openai}") String currentProviderLabel,
                         @Value("${app.chat.model-name:gpt-4o-mini}") String currentModelName,
                         @Value("${app.models.anthropic-model:claude-haiku-4-5-20251001}") String anthropicModel,
                         @Value("${app.models.gemini-model:gemini-2.5-flash}") String geminiModel,
                         @Value("${app.models.ollama-model:llama3.2}") String ollamaModel,
                         @Value("${app.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.selection = new Selection(LlmProvider.fromLabel(currentProviderLabel), currentModelName);
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.testAvailableModels = null;
        configuredModelNames.put(LlmProvider.OPENAI, currentModelName);
        configuredModelNames.put(LlmProvider.ANTHROPIC, anthropicModel);
        configuredModelNames.put(LlmProvider.GEMINI, geminiModel);
        configuredModelNames.put(LlmProvider.OLLAMA, ollamaModel);
    }

    /**
     * Package-private constructor for tests: pre-populated models (usually
     * fakes) keyed by {@code provider:model}, and the comparison set fixed to
     * those models so tests are independent of environment API keys.
     */
    ModelRegistry(LlmProvider currentProvider, String currentModelName, Map<String, ChatModel> models) {
        this.selection = new Selection(currentProvider, currentModelName);
        this.ollamaBaseUrl = "http://localhost:11434";
        this.testAvailableModels = models.keySet().stream().sorted().toList();
        this.models.putAll(models);
        for (LlmProvider provider : LlmProvider.values()) {
            configuredModelNames.put(provider, provider.defaultModelName());
        }
    }

    /**
     * The model used by the app right now, built lazily on first use. Reads the
     * selection snapshot once so the provider and model name are always paired
     * consistently (issue #253).
     */
    public ChatModel currentChatModel() {
        Selection s = selection;
        return getOrBuild(s.provider, s.modelName);
    }

    public LlmProvider currentProvider() {
        return selection.provider;
    }

    public String currentModelName() {
        return selection.modelName;
    }

    /**
     * Human-readable label of the current selection, e.g. {@code openai:gpt-4o-mini}.
     */
    public String currentLabel() {
        return key(selection.provider, selection.modelName);
    }

    /**
     * Switches the selection given a {@code provider:model} spec. A bare
     * {@code provider} uses that provider's configured default model.
     */
    public void setModel(String spec) {
        String trimmed = spec.trim();
        String[] parts = trimmed.split(":", 2);
        LlmProvider provider = LlmProvider.fromLabel(parts[0]);
        String modelName = parts.length > 1 && !parts[1].isBlank()
                ? parts[1].trim()
                : configuredModelNames.get(provider);
        setModel(provider, modelName);
    }

    public void setModel(LlmProvider provider, String modelName) {
        this.selection = new Selection(provider, modelName);
    }

    /**
     * Resolves a {@code provider:model} (or bare {@code provider}) spec to a
     * chat model without changing the current selection (issue #267). Used to
     * run a comparison against isolated model instances while live traffic
     * keeps using the shared {@link #currentChatModel()}. A bare
     * {@code provider} uses that provider's configured default model.
     */
    public ChatModel chatModelFor(String spec) {
        String trimmed = spec.trim();
        String[] parts = trimmed.split(":", 2);
        LlmProvider provider = LlmProvider.fromLabel(parts[0]);
        String modelName = parts.length > 1 && !parts[1].isBlank()
                ? parts[1].trim()
                : configuredModelNames.get(provider);
        return getOrBuild(provider, modelName);
    }

    /**
     * Returns a cached {@code provider:model} if one exists, otherwise builds it.
     * A build is only cached when it can actually work: for providers that need
     * an API key, the model is built from the live key each call until a key is
     * present, so a transiently missing/blank key is never cached (issue #270).
     * Without this, a misconfigured first call would poison the cache for the
     * life of the process.
     */
    private ChatModel getOrBuild(LlmProvider provider, String modelName) {
        String builtKey = key(provider, modelName);
        ChatModel cached = models.get(builtKey);
        if (cached != null) {
            return cached;
        }
        ChatModel built = buildChatModel(provider, modelName);
        if (needsKey(provider) && !hasApiKey(provider)) {
            return built;
        }
        models.putIfAbsent(builtKey, built);
        return built;
    }

    /**
     * The {@code provider:model} pairs available for comparison: every provider
     * whose API key is set in the environment, plus Ollama. Returns the
     * provider's configured default model.
     */
    public List<String> availableModels() {
        if (testAvailableModels != null) {
            return testAvailableModels;
        }
        List<String> available = new ArrayList<>();
        for (LlmProvider provider : LlmProvider.values()) {
            if (provider == LlmProvider.OLLAMA || hasApiKey(provider)) {
                available.add(key(provider, configuredModelNames.get(provider)));
            }
        }
        return available;
    }

    /**
     * All providers with their configured default model and whether the
     * provider is usable (key present, or local Ollama).
     */
    public Map<String, String> modelList() {
        Map<String, String> list = new LinkedHashMap<>();
        for (LlmProvider provider : LlmProvider.values()) {
            String availability = provider == LlmProvider.OLLAMA ? "local"
                    : hasApiKey(provider) ? "ready" : "no api key";
            list.put(key(provider, configuredModelNames.get(provider)), availability);
        }
        return list;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        return currentChatModel().doChat(chatRequest);
    }

    /**
     * The interface default returns {@code DefaultChatRequestParameters}, which
     * provider models cannot process: {@code OpenAiChatModel} casts the merged
     * parameters to {@code OpenAiChatRequestParameters} in {@code doChat}.
     * Delegating to the current model makes every request carry the provider's
     * own parameters.
     */
    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return currentChatModel().defaultRequestParameters();
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return currentChatModel().supportedCapabilities();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return currentChatModel().listeners();
    }

    @Override
    public ModelProvider provider() {
        return currentChatModel().provider();
    }

    protected ChatModel buildChatModel(LlmProvider provider, String modelName) {
        return switch (provider) {
            case OPENAI -> OpenAiChatModel.builder()
                    .apiKey(apiKeyFor(provider))
                    .modelName(modelName)
                    .build();
            case ANTHROPIC -> AnthropicChatModel.builder()
                    .apiKey(apiKeyFor(provider))
                    .modelName(modelName)
                    .build();
            case GEMINI -> GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKeyFor(provider))
                    .modelName(modelName)
                    .build();
            case OLLAMA -> OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(modelName)
                    .build();
        };
    }

    /**
     * The provider's API key, read from the environment. Extracted so tests can
     * override it to exercise the no-key, do-not-cache path (issue #270).
     */
    protected String apiKeyFor(LlmProvider provider) {
        return switch (provider) {
            case OPENAI -> System.getenv("OPENAI_API_KEY");
            case ANTHROPIC -> System.getenv("ANTHROPIC_API_KEY");
            case GEMINI -> System.getenv("GOOGLE_AI_GEMINI_API_KEY");
            case OLLAMA -> null;
        };
    }

    private boolean hasApiKey(LlmProvider provider) {
        String key = apiKeyFor(provider);
        return key != null && !key.isBlank();
    }

    /** Whether {@code provider} requires an API key (everything but local Ollama). */
    private static boolean needsKey(LlmProvider provider) {
        return provider != LlmProvider.OLLAMA;
    }

    private static String key(LlmProvider provider, String modelName) {
        return provider.label() + ":" + modelName;
    }
}
