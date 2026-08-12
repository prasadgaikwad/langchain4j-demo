package dev.prasadgaikwad.langchain4jdemo.evaluation;

import java.util.List;
import java.util.Map;

/**
 * The aggregate result of running an evaluation over a golden dataset: one
 * {@link EvaluationReportItem} per question plus the average score per metric.
 *
 * @param dataset      the name of the golden dataset that was evaluated
 * @param items        the per-question results
 * @param averageScores average score per metric across all questions
 */
public record EvaluationReport(String dataset, List<EvaluationReportItem> items,
                               Map<String, Double> averageScores) {

    /**
     * Number of questions in the dataset.
     */
    public int sampleCount() {
        return items.size();
    }
}
