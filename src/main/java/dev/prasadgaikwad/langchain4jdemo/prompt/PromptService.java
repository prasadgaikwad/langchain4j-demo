package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Demonstrates prompt templates: reusable prompt skeletons with
 * {@code {{placeholders}}}. {@link PromptTemplate} renders a template into a
 * {@link Prompt}, which can be turned into a {@link SystemMessage} or
 * {@link UserMessage}. No chat model is involved here — it is a pure,
 * offline-testable stage of prompt engineering.
 */
@Service
public class PromptService {

    private static final String SYSTEM_TEMPLATE = """
            You are a professional movie critic.
            Always write your reviews in a {{tone}} tone.
            """;

    private static final String USER_TEMPLATE = """
            Write a short review for the movie "{{movie}}" ({{year}}).
            Include a rating out of 10 in your review.
            """;

    /**
     * Renders the system and user templates into chat messages.
     */
    public List<ChatMessage> buildMovieReviewMessages(String movie, int year, String tone) {
        Prompt systemPrompt = PromptTemplate.from(SYSTEM_TEMPLATE).apply(Map.of("tone", tone));
        Prompt userPrompt = PromptTemplate.from(USER_TEMPLATE).apply(Map.of("movie", movie, "year", year));
        return List.of(systemPrompt.toSystemMessage(), userPrompt.toUserMessage());
    }

    /**
     * Renders the same templates into a single displayable string, so the CLI
     * can show exactly what would be sent to the model without calling it.
     */
    public String renderMovieReviewPrompt(String movie, int year, String tone) {
        List<ChatMessage> messages = buildMovieReviewMessages(movie, year, tone);
        StringBuilder rendered = new StringBuilder();
        for (ChatMessage message : messages) {
            rendered.append(message.type().toString()).append(":\n");
            rendered.append(textOf(message)).append("\n\n");
        }
        return rendered.toString().trim();
    }

    private static String textOf(ChatMessage message) {
        return switch (message.type()) {
            case SYSTEM -> ((SystemMessage) message).text();
            case USER -> ((UserMessage) message).singleText();
            default -> message.toString();
        };
    }
}
