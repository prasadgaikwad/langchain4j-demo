package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resume request for a paused human-in-the-loop pipeline")
public record HitlResumeRequest(
        @Schema(description = "Session ID of the paused run", example = "session-123") String sessionId,
        @Schema(description = "Whether to approve the proposed action", example = "true") boolean approved,
        @Schema(description = "Optional feedback explaining the decision", example = "Use metric units") String feedback
) {}
