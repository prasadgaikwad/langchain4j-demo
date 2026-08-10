package dev.prasadgaikwad.langchain4jdemo.evaluation;

/**
 * A single evaluation metric. Given a golden question, its expected answer, and
 * the answer actually produced by an AI system, returns a score in the range
 * {@code [0, 1]} where higher means better.
 */
public interface Metric {

    /**
     * @return a human-readable name for this metric, used as a column header in reports
     */
    String name();

    /**
     * Scores one answer.
     *
     * @param question the question (or input text) the answer was produced for
     * @param expected the expected/ground-truth answer
     * @param actual   the answer produced by the system under evaluation
     * @return a score between {@code 0.0} (worst) and {@code 1.0} (best)
     */
    double evaluate(String question, String expected, String actual);
}
