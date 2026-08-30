package dev.prasadgaikwad.langchain4jdemo.llm;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.ai.QaAssistant;
import dev.prasadgaikwad.langchain4jdemo.prompt.FewShotAssistant;

/**
 * Builds fresh AI-service instances for an arbitrary {@link ChatModel}.
 *
 * <p>Enables #267: a model comparison runs each candidate model against its own
 * isolated {@code Assistant}/{@code QaAssistant}/{@code FewShotAssistant}
 * instances instead of mutating the shared {@link ModelRegistry} selection, so
 * live traffic is never repointed mid-comparison. Memory and retrieval
 * augmentation are model-independent and shared across models.</p>
 */
public class ModelScopedServices {

    private final ChatMemoryProvider chatMemoryProvider;
    private final RetrievalAugmentor retrievalAugmentor;

    public ModelScopedServices(ChatMemoryProvider chatMemoryProvider, RetrievalAugmentor retrievalAugmentor) {
        this.chatMemoryProvider = chatMemoryProvider;
        this.retrievalAugmentor = retrievalAugmentor;
    }

    public Assistant assistant(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    public QaAssistant qaAssistant(ChatModel chatModel) {
        return AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    public FewShotAssistant fewShotAssistant(ChatModel chatModel) {
        return AiServices.builder(FewShotAssistant.class)
                .chatModel(chatModel)
                .build();
    }
}
