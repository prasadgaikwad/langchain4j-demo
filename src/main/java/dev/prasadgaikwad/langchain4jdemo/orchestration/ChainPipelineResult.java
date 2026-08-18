package dev.prasadgaikwad.langchain4jdemo.orchestration;

/**
 * Complete output of the sequential blog-post pipeline. Each field captures
 * one agent's contribution, giving the caller full visibility into the
 * pipeline trace.
 */
public record ChainPipelineResult(
        String topic,
        String outline,
        String draft,
        String edited,
        String formatted) {
}
