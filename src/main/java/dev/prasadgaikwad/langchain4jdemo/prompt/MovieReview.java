package dev.prasadgaikwad.langchain4jdemo.prompt;

/**
 * Structured output produced by {@link MovieExtractor}. Returning a POJO (here
 * a record) from an AI Service makes LangChain4j request JSON matching this
 * type's schema and parse the model reply into an instance.
 */
public record MovieReview(String title, int year, String director, double rating, String summary) {
}
