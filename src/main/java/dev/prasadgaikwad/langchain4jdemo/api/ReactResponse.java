package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "LangGraph4j ReACT agent result with step trace")
public record ReactResponse(
        @Schema(description = "The original task") String task,
        @Schema(description = "The agent's final answer") String answer,
        @Schema(description = "Graph nodes executed (agent, action, agent, ...)") List<String> steps,
        @Schema(description = "Agent messages + tool-call/tool-result entries from the ReACT loop")
        List<String> agentTrace
) {}
