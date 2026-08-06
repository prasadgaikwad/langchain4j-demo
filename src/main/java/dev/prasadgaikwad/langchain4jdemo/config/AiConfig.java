package dev.prasadgaikwad.langchain4jdemo.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.EmbeddingStoreStatsTool;
import dev.prasadgaikwad.langchain4jdemo.ai.Agent;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.ai.QaAssistant;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import dev.prasadgaikwad.langchain4jdemo.rag.SemanticSearchContentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class AiConfig {

    @Bean
    public ChatModel chatModel(@Value("${app.chat.model-name:gpt-4o-mini}") String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(modelName)
                .build();
    }

    @Bean
    public Assistant assistant(ChatModel chatModel,
                               ChatMemoryRegistry chatMemoryRegistry,
                               @Value("${app.chat.model-name:gpt-4o-mini}") String modelName,
                               @Value("${app.memory.max-messages:10}") int maxMessages,
                               @Value("${app.memory.max-tokens:2000}") int maxTokens) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(createChatMemoryProvider(chatMemoryRegistry, modelName, maxMessages, maxTokens))
                .build();
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
     * Question-answering AI service: chats with memory and answers from the
     * documents retrieved by the {@link RetrievalAugmentor}.
     */
    @Bean
    public QaAssistant qaAssistant(ChatModel chatModel,
                                   RetrievalAugmentor retrievalAugmentor,
                                   ChatMemoryRegistry chatMemoryRegistry,
                                   @Value("${app.chat.model-name:gpt-4o-mini}") String modelName,
                                   @Value("${app.memory.max-messages:10}") int maxMessages,
                                   @Value("${app.memory.max-tokens:2000}") int maxTokens) {
        return AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(createChatMemoryProvider(chatMemoryRegistry, modelName, maxMessages, maxTokens))
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
