package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.prompt.FewShotAssistant;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieExtractor;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieReview;
import dev.prasadgaikwad.langchain4jdemo.prompt.PromptService;
import dev.prasadgaikwad.langchain4jdemo.prompt.Sentiment;
import dev.prasadgaikwad.langchain4jdemo.prompt.TopicExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the prompting techniques: few-shot classification,
 * structured movie extraction, topic extraction, and template rendering.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Prompting", description = "Prompt templates, few-shot classification, and output parsers")
public class PromptApiController {

    private final FewShotAssistant fewShotAssistant;
    private final MovieExtractor movieExtractor;
    private final TopicExtractor topicExtractor;
    private final PromptService promptService;

    public PromptApiController(FewShotAssistant fewShotAssistant,
                               MovieExtractor movieExtractor,
                               TopicExtractor topicExtractor,
                               PromptService promptService) {
        this.fewShotAssistant = fewShotAssistant;
        this.movieExtractor = movieExtractor;
        this.topicExtractor = topicExtractor;
        this.promptService = promptService;
    }

    @PostMapping("/sentiment")
    @Operation(summary = "Classify text sentiment with few-shot examples",
            description = "Returns one of POSITIVE, NEGATIVE, or NEUTRAL using a "
                    + "few-shot classifier that constrains the model to the enum.")
    @ApiResponse(responseCode = "200", description = "The classified sentiment",
            content = @Content(schema = @Schema(implementation = Sentiment.class)))
    public Sentiment sentiment(@RequestBody TextRequest request) {
        return fewShotAssistant.classify(request.text());
    }

    @PostMapping("/movie")
    @Operation(summary = "Extract structured movie data",
            description = "Parses a movie description into a typed MovieReview record via an output parser.")
    @ApiResponse(responseCode = "200", description = "The extracted movie data",
            content = @Content(schema = @Schema(implementation = MovieReview.class)))
    public MovieReview movie(@RequestBody TextRequest request) {
        return movieExtractor.extract(request.text());
    }

    @PostMapping("/topics")
    @Operation(summary = "Extract a list of topics",
            description = "Parses the text into a list of topic strings via an output parser.")
    @ApiResponse(responseCode = "200", description = "The extracted topics",
            content = @Content(schema = @Schema(implementation = List.class)))
    public List<String> topics(@RequestBody TextRequest request) {
        return topicExtractor.extract(request.text());
    }

    @GetMapping("/template")
    @Operation(summary = "Render a prompt template (no API call)",
            description = "Renders the movie-review prompt template with fixed example values; "
                    + "purely offline, useful for inspecting what is sent to the model.")
    @ApiResponse(responseCode = "200", description = "The rendered system and user messages")
    public String template(@Parameter(description = "Movie title to render into the template",
            example = "Inception")
                           @RequestParam(defaultValue = "Inception") String movie) {
        return promptService.renderMovieReviewPrompt(movie, 2010, "enthusiastic");
    }
}
