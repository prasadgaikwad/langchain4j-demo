package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FewShotAssistantTest {

    @Test
    void embedsFewShotExamplesInTheSystemMessage() {
        FakeChatModel chatModel = new FakeChatModel("POSITIVE");
        FewShotAssistant assistant = AiServices.builder(FewShotAssistant.class)
                .chatModel(chatModel)
                .build();

        Sentiment sentiment = assistant.classify("This movie is amazing!");

        assertThat(chatModel.lastSystemMessage())
                .contains("Examples:")
                .contains("I absolutely loved this movie")
                .contains("Sentiment: NEGATIVE");
        assertThat(chatModel.lastUserMessage()).contains("This movie is amazing!");
        assertThat(sentiment).isEqualTo(Sentiment.POSITIVE);
    }

    @Test
    void parsesTheModelReplyIntoTheEnumConstant() {
        FakeChatModel chatModel = new FakeChatModel("NEUTRAL");
        FewShotAssistant assistant = AiServices.builder(FewShotAssistant.class)
                .chatModel(chatModel)
                .build();

        Sentiment sentiment = assistant.classify("It arrived on time.");

        assertThat(sentiment).isEqualTo(Sentiment.NEUTRAL);
    }
}
