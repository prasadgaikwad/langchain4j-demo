package dev.prasadgaikwad.langchain4jdemo.structured;

import tools.jackson.databind.json.JsonMapper;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieReview;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaExtractionServiceTest {

    private static final String MOVIE_JSON = """
            {
              "title": "Inception",
              "year": 2010,
              "director": "Christopher Nolan",
              "rating": 9.0,
              "summary": "A thief enters dreams to plant an idea."
            }
            """;

    @Test
    void attachesTheJsonSchemaResponseFormatToTheRequest() {
        FakeChatModel chatModel = new FakeChatModel(MOVIE_JSON);
        JsonSchemaExtractionService service = new JsonSchemaExtractionService(chatModel, new JsonMapper());

        service.extractMovie("Tell me about Inception.");

        JsonSchema schema = chatModel.lastRequest().parameters().responseFormat().jsonSchema();
        assertThat(schema).isNotNull();
        assertThat(schema.name()).isEqualTo("MovieReview");
    }

    @Test
    void parsesTheModelReplyIntoTheMovieReviewRecord() {
        FakeChatModel chatModel = new FakeChatModel(MOVIE_JSON);
        JsonSchemaExtractionService service = new JsonSchemaExtractionService(chatModel, new JsonMapper());

        MovieReview review = service.extractMovie("Tell me about Inception.");

        assertThat(review.title()).isEqualTo("Inception");
        assertThat(review.year()).isEqualTo(2010);
        assertThat(review.rating()).isEqualTo(9.0);
    }

    @Test
    void movieReviewSchemaContainsTheExpectedFields() {
        JsonSchema schema = JsonSchemaExtractionService.movieReviewSchema();

        assertThat(schema.name()).isEqualTo("MovieReview");
        assertThat(schema.rootElement().toString()).contains("title", "year", "director", "rating", "summary");
    }
}
