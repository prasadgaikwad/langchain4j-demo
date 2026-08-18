package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for the {@code POST /api/chain} endpoint. Contains the full
 * pipeline trace: the topic, each intermediate agent's output, and the final
 * formatted blog post.
 */
@Schema(description = "Full output of the sequential chain-of-agents pipeline")
public record ChainResponse(
        @Schema(description = "The input topic", example = "Spring Boot 4 migration")
        String topic,

        @Schema(description = "Outline produced by the OutlineAgent", example = "# Spring Boot 4 Migration\n\n...")
        String outline,

        @Schema(description = "Draft written by the DraftAgent")
        String draft,

        @Schema(description = "Edited version produced by the EditorAgent")
        String edited,

        @Schema(description = "Final formatted blog post from the FormatAgent")
        String formatted) {
}
