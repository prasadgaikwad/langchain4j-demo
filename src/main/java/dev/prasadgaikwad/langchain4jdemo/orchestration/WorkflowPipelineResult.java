package dev.prasadgaikwad.langchain4jdemo.orchestration;

import java.util.List;

public record WorkflowPipelineResult(
        String topic,
        String research,
        String draft,
        String formatted,
        int refinementIterations,
        String category,
        List<String> executedAgents
) {}
