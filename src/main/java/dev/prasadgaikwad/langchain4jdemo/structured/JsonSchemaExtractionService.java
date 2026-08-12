package dev.prasadgaikwad.langchain4jdemo.structured;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.service.output.JsonSchemas;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieReview;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Structured-output capability at the model level: rather than asking for JSON
 * in the prompt and parsing it (as {@code MovieExtractor} does), this service
 * attaches the JSON schema derived from {@link MovieReview} to the chat request
 * via the response format, so the model is constrained to emit exactly that
 * shape. The reply is then parsed with Jackson.
 */
@Service
public class JsonSchemaExtractionService {

    private static final String SYSTEM_PROMPT = """
            Extract the requested fields from the given text and return a JSON object
            that conforms to the provided schema. Do not add fields that are not in the schema.
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public JsonSchemaExtractionService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts a {@link MovieReview} from the text, using the JSON schema
     * derived from the record type as the model's response format.
     */
    public MovieReview extractMovie(String text) {
        JsonSchema schema = JsonSchemas.jsonSchemaFrom(MovieReview.class)
                .orElseThrow(() -> new IllegalStateException("No JSON schema derivable from MovieReview"));
        String reply = chatModel.chat(ChatRequest.builder()
                        .messages(List.of(
                                SystemMessage.from(SYSTEM_PROMPT),
                                UserMessage.from(text)))
                        .parameters(ChatRequestParameters.builder()
                                .responseFormat(schema)
                                .build())
                        .build())
                .aiMessage().text();
        return parse(reply);
    }

    /**
     * The JSON schema used for {@code MovieReview}, exposed for tests and to
     * inspect what the model is asked to conform to.
     */
    public static JsonSchema movieReviewSchema() {
        return JsonSchemas.jsonSchemaFrom(MovieReview.class)
                .orElseThrow(() -> new IllegalStateException("No JSON schema derivable from MovieReview"));
    }

    private MovieReview parse(String reply) {
        try {
            return objectMapper.readValue(reply, MovieReview.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Model reply is not valid MovieReview JSON: " + reply, e);
        }
    }
}
