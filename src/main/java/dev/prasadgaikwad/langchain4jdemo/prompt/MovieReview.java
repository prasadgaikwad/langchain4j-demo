package dev.prasadgaikwad.langchain4jdemo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Structured output produced by {@link MovieExtractor}. Returning a POJO (here
 * a record) from an AI Service makes LangChain4j request JSON matching this
 * type's schema and parse the model reply into an instance.
 */
@Schema(description = "Structured movie data extracted by the AI")
public record MovieReview(
        @Schema(description = "Movie title", example = "Inception")
        String title,
        @Schema(description = "Release year", example = "2010")
        int year,
        @Schema(description = "Director", example = "Christopher Nolan")
        String director,
        @Schema(description = "Rating out of 10", example = "9.0")
        double rating,
        @Schema(description = "One-sentence summary", example = "A thief enters dreams to plant an idea.")
        String summary) {
}
