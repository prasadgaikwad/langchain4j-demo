package dev.prasadgaikwad.langchain4jdemo.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        ChatMemoryProvider chatMemoryProvider = memoryId -> {
            ChatMemory chatMemory = createMemory((String) memoryId, modelName, maxMessages, maxTokens);
            chatMemoryRegistry.register((String) memoryId, chatMemory);
            return chatMemory;
        };

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
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
