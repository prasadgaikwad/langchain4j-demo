package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.prompt.FewShotAssistant;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieExtractor;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieReview;
import dev.prasadgaikwad.langchain4jdemo.prompt.PromptService;
import dev.prasadgaikwad.langchain4jdemo.prompt.Sentiment;
import dev.prasadgaikwad.langchain4jdemo.prompt.TopicExtractor;
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
    public Sentiment sentiment(@RequestBody TextRequest request) {
        return fewShotAssistant.classify(request.text());
    }

    @PostMapping("/movie")
    public MovieReview movie(@RequestBody TextRequest request) {
        return movieExtractor.extract(request.text());
    }

    @PostMapping("/topics")
    public List<String> topics(@RequestBody TextRequest request) {
        return topicExtractor.extract(request.text());
    }

    @GetMapping("/template")
    public String template(@RequestParam(defaultValue = "Inception") String movie) {
        return promptService.renderMovieReviewPrompt(movie, 2010, "enthusiastic");
    }
}
