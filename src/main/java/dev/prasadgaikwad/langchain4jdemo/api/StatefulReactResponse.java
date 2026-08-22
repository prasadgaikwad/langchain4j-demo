package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LangGraph4j stateful pipeline result with checkpoint history")
public record StatefulReactResponse(
        @Schema(description = "Session ID (thread) for this pipeline run") String sessionId,
        @Schema(description = "The original task") String task,
        @Schema(description = "The agent's final answer") String answer,
        @Schema(description = "Graph nodes executed in this run") java.util.List<String> steps,
        @Schema(description = "Total checkpoints saved for this session") int checkpointCount,
        @Schema(description = "Full checkpoint history for this session") java.util.List<StatefulReactResponse.StateEntry> history
) {
    @Schema(description = "A single checkpoint in the pipeline history")
    public record StateEntry(
            @Schema(description = "Session ID") String sessionId,
            @Schema(description = "Graph node at this checkpoint") String node,
            @Schema(description = "Last AI message at this checkpoint") String lastAiMessage,
            @Schema(description = "Total messages at this checkpoint") int messageCount
    ) {}
}
