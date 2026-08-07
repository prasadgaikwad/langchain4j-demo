package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovieExtractorTest {

    @Test
    void parsesTheModelReplyIntoTheMovieReviewRecord() {
        String cannedJson = """
                {
                  "title": "Inception",
                  "year": 2010,
                  "director": "Christopher Nolan",
                  "rating": 9.0,
                  "summary": "A thief enters dreams to plant an idea."
                }
                """;
        FakeChatModel chatModel = new FakeChatModel(cannedJson);
        MovieExtractor extractor = AiServices.builder(MovieExtractor.class)
                .chatModel(chatModel)
                .build();

        MovieReview review = extractor.extract("Tell me about Inception.");

        assertThat(review.title()).isEqualTo("Inception");
        assertThat(review.year()).isEqualTo(2010);
        assertThat(review.director()).isEqualTo("Christopher Nolan");
        assertThat(review.rating()).isEqualTo(9.0);
        assertThat(review.summary()).contains("dreams");
        assertThat(chatModel.lastSystemMessage()).contains("title (string)");
    }
}
