package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptServiceTest {

    private final PromptService promptService = new PromptService();

    @Test
    void rendersSystemAndUserTemplatesWithVariables() {
        List<ChatMessage> messages = promptService.buildMovieReviewMessages("Inception", 2010, "enthusiastic");

        assertThat(messages).hasSize(2);
        assertThat(((SystemMessage) messages.get(0)).text())
                .contains("enthusiastic tone")
                .contains("professional movie critic");
        assertThat(((UserMessage) messages.get(1)).singleText())
                .contains("Inception")
                .contains("(2010)");
    }

    @Test
    void differentVariablesProduceDifferentRenders() {
        List<ChatMessage> messages = promptService.buildMovieReviewMessages("Titanic", 1997, "sarcastic");

        assertThat(((SystemMessage) messages.get(0)).text()).contains("sarcastic tone");
        assertThat(((UserMessage) messages.get(1)).singleText())
                .contains("Titanic")
                .contains("(1997)")
                .doesNotContain("Inception");
    }

    @Test
    void renderProducesDisplayableText() {
        String rendered = promptService.renderMovieReviewPrompt("Inception", 2010, "enthusiastic");

        assertThat(rendered)
                .contains("SYSTEM")
                .contains("USER")
                .contains("Inception")
                .contains("(2010)");
    }
}
