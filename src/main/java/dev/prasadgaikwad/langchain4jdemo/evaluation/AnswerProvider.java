package dev.prasadgaikwad.langchain4jdemo.evaluation;

/**
 * How the system under evaluation is invoked: given a question, produce an
 * answer. Adapters let any AI service be evaluated, e.g. {@code QaService::ask}
 * for RAG, the {@code Assistant} for chat, or a sentiment classifier.
 */
@FunctionalInterface
public interface AnswerProvider {

    String answer(String question);
}
