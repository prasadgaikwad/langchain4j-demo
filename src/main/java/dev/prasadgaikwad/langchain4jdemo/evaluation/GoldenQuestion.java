package dev.prasadgaikwad.langchain4jdemo.evaluation;

/**
 * One entry of a golden dataset: a question (or input text) together with the
 * expected/ground-truth answer an AI system should produce.
 *
 * @param question       the question or input to send to the system
 * @param expectedAnswer the ground-truth answer used for scoring
 */
public record GoldenQuestion(String question, String expectedAnswer) {
}
