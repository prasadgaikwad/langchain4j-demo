package dev.prasadgaikwad.langchain4jdemo.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI Service interface backed by LangChain4j {@code AiServices}.
 * <p>
 * The {@link MemoryId} parameter selects a per-conversation {@code ChatMemory},
 * and {@link UserMessage} marks the user input. Each method call automatically
 * includes the stored conversation history.
 */
public interface Assistant {

    @SystemMessage("You are a helpful assistant. Answer the question in a very concise way, only in 2 sentences maximum.")
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
