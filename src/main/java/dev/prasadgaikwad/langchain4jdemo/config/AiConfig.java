package dev.prasadgaikwad.langchain4jdemo.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.audio.AudioTranscriptionModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiAudioTranscriptionModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DynamicToolProvider;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import dev.prasadgaikwad.langchain4jdemo.agentic.CrewService;
import dev.prasadgaikwad.langchain4jdemo.ai.Agent;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.ai.DynamicAgent;
import dev.prasadgaikwad.langchain4jdemo.ai.QaAssistant;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.llm.ModelRegistry;
import dev.prasadgaikwad.langchain4jdemo.llm.ModelScopedServices;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import dev.prasadgaikwad.langchain4jdemo.orchestration.BoundedMemorySaver;
import dev.prasadgaikwad.langchain4jdemo.prompt.FewShotAssistant;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieExtractor;
import dev.prasadgaikwad.langchain4jdemo.prompt.TopicExtractor;
import dev.prasadgaikwad.langchain4jdemo.rag.SemanticSearchContentRetriever;
import dev.prasadgaikwad.langchain4jdemo.streaming.StreamingAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class AiConfig {

    /**
     * The chat model used by every AI service is the {@code ModelRegistry}:
     * switching the registry's provider/model selection switches all services
     * (assistant, RAG, agent, few-shot, judge) at runtime. The registry is the
     * only {@code ChatModel} bean, so services autowire it by type.
     */
    @Bean
    public StreamingChatModel streamingChatModel(@Value("${app.chat.model-name:gpt-4o-mini}") String modelName) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(modelName)
                .build();
    }

    /**
     * Streaming AI service that can also call {@code @Tool} methods while tokens
     * are streamed back, demonstrating streaming function calling.
     */
    @Bean
    public StreamingAgent streamingAgent(StreamingChatModel streamingChatModel,
                                         ChatMemoryRegistry chatMemoryRegistry,
                                         CalculatorTool calculatorTool,
                                         @Value("${app.chat.model-name:gpt-4o-mini}") String modelName,
                                         @Value("${app.memory.max-messages:10}") int maxMessages,
                                         @Value("${app.memory.max-tokens:2000}") int maxTokens) {
        return AiServices.builder(StreamingAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(createChatMemoryProvider(chatMemoryRegistry, modelName, maxMessages, maxTokens))
                .tools(calculatorTool)
                .build();
    }

    @Bean
    public Assistant assistant(ChatModel chatModel,
                               ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    /**
     * Shared conversation memory, reused by every chat service and by
     * {@link ModelScopedServices} so per-model comparison instances share the
     * same per-conversation memory semantics.
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryRegistry chatMemoryRegistry,
                                                 @Value("${app.chat.model-name:gpt-4o-mini}") String modelName,
                                                 @Value("${app.memory.max-messages:10}") int maxMessages,
                                                 @Value("${app.memory.max-tokens:2000}") int maxTokens) {
        return createChatMemoryProvider(chatMemoryRegistry, modelName, maxMessages, maxTokens);
    }

    /**
     * Retrieves the most relevant document chunks for a question from the embedding
     * store. This is the retrieval stage of the RAG pipeline.
     */
    @Bean
    public ContentRetriever contentRetriever(SemanticSearchService searchService,
                                             @Value("${app.rag.max-results:5}") int maxResults) {
        return new SemanticSearchContentRetriever(searchService, maxResults);
    }

    /**
     * Composes the RAG pipeline: query transform, retrieval, aggregation, and
     * injection of the retrieved content into the user message.
     */
    @Bean
    public RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();
    }

    /**
     * Factory for building per-model AI-service instances without repointing the
     * shared {@link ModelRegistry}, used by model comparison (issue #267).
     */
    @Bean
    public ModelScopedServices modelScopedServices(ChatMemoryProvider chatMemoryProvider,
                                                   RetrievalAugmentor retrievalAugmentor) {
        return new ModelScopedServices(chatMemoryProvider, retrievalAugmentor);
    }

    /**
     * Bounded checkpoint store shared by the stateful and human-in-the-loop
     * graphs so long-running sessions cannot leak checkpoint memory without
     * bound (issue #252).
     */
    @Bean
    public BoundedMemorySaver checkpointSaver(
            @Value("${app.checkpoint.max-checkpoints:10000}") int maxCheckpoints) {
        return new BoundedMemorySaver(maxCheckpoints);
    }

    /**
     * Question-answering AI service: chats with memory and answers from the
     * documents retrieved by the {@link RetrievalAugmentor}.
     */
    @Bean
    public QaAssistant qaAssistant(ChatModel chatModel,
                                   RetrievalAugmentor retrievalAugmentor,
                                   ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    /**
     * Agent AI service: chats with memory and can call the registered
     * {@code @Tool} methods (calculator, document search, store stats) while
     * working on the task.
     */
    @Bean
    public Agent agent(ChatModel chatModel,
                       CalculatorTool calculatorTool,
                       DocumentSearchTool documentSearchTool,
                       EmbeddingStoreStatsTool storeStatsTool,
                       ChatMemoryRegistry chatMemoryRegistry,
                       @Value("${app.chat.model-name:gpt-4o-mini}") String modelName,
                       @Value("${app.memory.max-messages:10}") int maxMessages,
                       @Value("${app.memory.max-tokens:2000}") int maxTokens) {
        return AiServices.builder(Agent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(createChatMemoryProvider(chatMemoryRegistry, modelName, maxMessages, maxTokens))
                .tools(calculatorTool, documentSearchTool, storeStatsTool)
                .build();
    }

    /**
     * Agent AI service whose tools are chosen per request by a
     * {@link DynamicToolProvider} instead of a fixed build-time set.
     */
    @Bean
    public DynamicAgent dynamicAgent(ChatModel chatModel,
                                     DynamicToolProvider toolProvider,
                                     ChatMemoryRegistry chatMemoryRegistry,
                                     @Value("${app.chat.model-name:gpt-4o-mini}") String modelName,
                                     @Value("${app.memory.max-messages:10}") int maxMessages,
                                     @Value("${app.memory.max-tokens:2000}") int maxTokens) {
        return AiServices.builder(DynamicAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(createChatMemoryProvider(chatMemoryRegistry, modelName, maxMessages, maxTokens))
                .toolProvider(toolProvider)
                .build();
    }

    /**
     * Image model used by the multi-modal demo to generate images from text
     * prompts ({@code gpt-image-1} by default).
     */
    @Bean
    public ImageModel imageModel(@Value("${app.image.model-name:gpt-image-1}") String modelName) {
        return OpenAiImageModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(modelName)
                .build();
    }

    /**
     * Audio transcription model used by the speech-to-text demo
     * ({@code whisper-1} by default).
     */
    @Bean
    public AudioTranscriptionModel audioTranscriptionModel(
            @Value("${app.stt.model-name:whisper-1}") String modelName) {
        return OpenAiAudioTranscriptionModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(modelName)
                .build();
    }

    /**
     * Few-shot classification AI service: the system message embeds labeled
     * examples and the {@code Sentiment} return type parses the reply.
     */
    @Bean
    public FewShotAssistant fewShotAssistant(ChatModel chatModel) {
        return AiServices.builder(FewShotAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * Structured-output AI service: returns a {@code MovieReview} record parsed
     * from the model's JSON reply.
     */
    @Bean
    public MovieExtractor movieExtractor(ChatModel chatModel) {
        return AiServices.builder(MovieExtractor.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * Collection-output AI service: returns a {@code List<String>} parsed from
     * a JSON array in the model's reply.
     */
    @Bean
    public TopicExtractor topicExtractor(ChatModel chatModel) {
        return AiServices.builder(TopicExtractor.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * Factory that builds an {@link EmbeddingModel} for a given model name.
     * Exposed as a bean so the model can be switched at runtime (see {@code SemanticSearchService}).
     */
    @Bean
    public Function<String, EmbeddingModel> embeddingModelFactory() {
        return modelName -> OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(modelName)
                .build();
    }

    private ChatMemoryProvider createChatMemoryProvider(ChatMemoryRegistry chatMemoryRegistry,
                                                        String modelName,
                                                        int maxMessages,
                                                        int maxTokens) {
        return memoryId -> {
            ChatMemory chatMemory = createMemory((String) memoryId, modelName, maxMessages, maxTokens);
            chatMemoryRegistry.register((String) memoryId, chatMemory);
            return chatMemory;
        };
    }

    private ChatMemory createMemory(String memoryId, String modelName, int maxMessages, int maxTokens) {
        if (memoryId.startsWith(MemoryType.MESSAGE_WINDOW.label())) {
            return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(maxMessages)
                    .build();
        }
        return TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(maxTokens, new OpenAiTokenCountEstimator(modelName))
                .build();
    }
}
