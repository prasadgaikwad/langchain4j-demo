package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LangGraph4j human-in-the-loop result — may be awaiting approval")
public record HitlResponse(
        @Schema(description = "Session ID (thread) for this pipeline run") String sessionId,
        @Schema(description = "The original task") String task,
        @Schema(description = "The agent's final answer (empty when awaiting approval)") String answer,
        @Schema(description = "Graph nodes executed in this run") java.util.List<String> steps,
        @Schema(description = "True when the graph is paused awaiting human approval") boolean awaitingApproval,
        @Schema(description = "The action the agent proposed (when awaiting approval)") String proposedAction,
        @Schema(description = "Human feedback provided on resume") String feedback
) {}
