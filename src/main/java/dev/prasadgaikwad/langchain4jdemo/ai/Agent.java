package dev.prasadgaikwad.langchain4jdemo.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent AI Service interface backed by {@code AiServices}, wired with tools.
 * <p>
 * Unlike {@link Assistant} and {@link QaAssistant}, this service is built with
 * {@code AiServices.builder(Agent.class).tools(...)}: the chat model can call
 * the registered {@code @Tool} methods while working on the task, turning a
 * single reply into a loop of "reason, call tool, observe result" steps.
 */
public interface Agent {

    @SystemMessage("""
            You are an agent that accomplishes the user's task using the available tools.
            Use the "searchDocuments" tool when the task asks about the indexed documents or your own data.
            Use the "calculate" tool for arithmetic computations.
            Use the "getEmbeddingStoreStats" tool when asked about the embedding store or the embedding model.
            If the task does not need a tool, answer directly. Be concise.
            """)
    String execute(@MemoryId String memoryId, @UserMessage String task);
}
