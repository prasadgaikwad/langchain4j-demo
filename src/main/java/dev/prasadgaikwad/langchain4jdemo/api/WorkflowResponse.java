package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Workflow composition result with parallel research, iterative refinement, "
        + "and conditional formatting pipeline trace")
public record WorkflowResponse(
        @Schema(description = "The original topic") String topic,
        @Schema(description = "Combined research from parallel agents") String research,
        @Schema(description = "The refined draft after iterative improvement") String draft,
        @Schema(description = "The final formatted blog post") String formatted,
        @Schema(description = "Number of refinement loop iterations performed") int refinementIterations,
        @Schema(description = "Detected topic category (technical or general)") String category,
        @Schema(description = "List of agents executed in the pipeline") java.util.List<String> executedAgents
) {}
