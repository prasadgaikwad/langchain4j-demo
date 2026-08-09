package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for chat, RAG, and agent endpoints.
 *
 * @param conversationId the conversation (memory) id; defaults to {@code "api"}
 * @param message        the user message or task
 */
@Schema(description = "A chat request shared by the /chat, /ask, and /agent endpoints")
public record ChatRequest(
        @Schema(description = "Conversation (memory) id; a fresh id starts a new conversation",
                example = "web", defaultValue = "api")
        String conversationId,
        @Schema(description = "The user message or task to run", example = "Hello!")
        String message) {

    public ChatRequest {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "api";
        }
    }
}
