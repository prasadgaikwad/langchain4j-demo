package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A request carrying a single piece of text, used by the prompt endpoints")
public record TextRequest(
        @Schema(description = "The text to classify or extract from", example = "I absolutely loved this movie!")
        String text) {
}
