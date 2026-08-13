package dev.prasadgaikwad.langchain4jdemo.llm;

import java.util.Map;

/**
 * The averaged metric scores produced by one model for a comparison run.
 *
 * @param model  the {@code provider:model} label
 * @param scores the per-metric averages, in {@code [0, 1]}
 */
public record ModelScore(String model, Map<String, Double> scores) {
}
