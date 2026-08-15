package dev.prasadgaikwad.langchain4jdemo.llm;

import java.util.List;

/**
 * Result of evaluating a golden dataset against several models: one
 * {@link ModelScore} row per model, ready to print as a comparison table.
 *
 * @param dataset the golden dataset that was evaluated
 * @param models  one row per model, in comparison order
 */
public record ComparisonReport(String dataset, List<ModelScore> models) {
}
