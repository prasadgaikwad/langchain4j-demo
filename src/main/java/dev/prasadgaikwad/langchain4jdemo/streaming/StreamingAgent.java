package dev.prasadgaikwad.langchain4jdemo.streaming;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI service that streams the model's reply token by token while still being
 * able to call the registered {@code @Tool} methods, demonstrating streaming
 * function calling.
 */
public interface StreamingAgent {

    TokenStream chat(@MemoryId String memoryId, @UserMessage String message);
}
