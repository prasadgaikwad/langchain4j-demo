package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Graph-of-agents pipeline response with full trace")
public record GraphResponse(
        @Schema(description = "The original prompt") String prompt,
        @Schema(description = "Extracted user profile") String profile,
        @Schema(description = "Suggested topic") String topic,
        @Schema(description = "Blog post outline") String outline,
        @Schema(description = "First draft") String draft,
        @Schema(description = "Edited content") String edited,
        @Schema(description = "Final personalized writeup") String writeup,
        @Schema(description = "Sequence of agents invoked by the planner") List<String> agentPath
) {}
