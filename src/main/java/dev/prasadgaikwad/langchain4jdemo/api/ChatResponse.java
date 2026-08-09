package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The assistant's answer")
public record ChatResponse(
        @Schema(description = "The generated answer text", example = "Hello! How can I help?")
        String answer) {
}
