package dev.prasadgaikwad.langchain4jdemo.orchestration;

import java.util.List;

public record GraphPipelineResult(
        String prompt,
        String profile,
        String topic,
        String outline,
        String draft,
        String edited,
        String writeup,
        List<String> agentPath
) {}
