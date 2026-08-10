package dev.prasadgaikwad.langchain4jdemo.evaluation;

import java.util.Map;

/**
 * The per-question result of an evaluation: the question, both the expected and
 * the produced answer, and one score per metric.
 *
 * @param question       the question that was asked
 * @param expected       the ground-truth answer
 * @param actual         the answer produced by the system under evaluation
 * @param scores         metric name to score ({@code [0, 1]})
 */
public record EvaluationReportItem(String question, String expected, String actual,
                                   Map<String, Double> scores) {
}
