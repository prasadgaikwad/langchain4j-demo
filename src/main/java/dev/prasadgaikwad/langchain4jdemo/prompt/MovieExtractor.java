package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Structured-output AI Service. The {@link MovieReview} return type is the
 * output parser: LangChain4j derives a JSON schema from the record, asks the
 * model for JSON matching it, and parses the reply into a {@link MovieReview}
 * instance.
 */
public interface MovieExtractor {

    @SystemMessage("""
            Extract information about the movie from the given text.
            Return the data as a JSON object with exactly these fields:
            title (string), year (integer), director (string), rating (number from 1 to 10), summary (string).
            """)
    MovieReview extract(@UserMessage String text);
}
