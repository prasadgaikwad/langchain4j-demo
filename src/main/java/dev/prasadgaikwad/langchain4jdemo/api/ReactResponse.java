package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LangGraph4j ReACT agent result with step trace")
public record ReactResponse(
        @Schema(description = "The original task") String task,
        @Schema(description = "The agent's final answer") String answer,
        @Schema(description = "Graph nodes executed (agent, action, agent, ...)") java.util.List<String> steps,
        @Schema(description = "All AI messages from the ReACT loop") java.util.List<String> agentMessages
) {}
