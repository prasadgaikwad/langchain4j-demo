package dev.prasadgaikwad.langchain4jdemo.api;

/**
 * Request body for chat, RAG, and agent endpoints.
 *
 * @param conversationId the conversation (memory) id; defaults to {@code "api"}
 * @param message        the user message or task
 */
public record ChatRequest(String conversationId, String message) {

    public ChatRequest {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "api";
        }
    }
}
