package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopicExtractorTest {

    @Test
    void parsesTheModelReplyIntoAListOfStrings() {
        FakeChatModel chatModel = new FakeChatModel("java\nai\nspring");
        TopicExtractor extractor = AiServices.builder(TopicExtractor.class)
                .chatModel(chatModel)
                .build();

        List<String> topics = extractor.extract("This article covers Java, AI, and Spring Boot.");

        assertThat(topics).containsExactly("java", "ai", "spring");
    }

    @Test
    void embedsTheUserTextInTheRequest() {
        FakeChatModel chatModel = new FakeChatModel("[]");
        TopicExtractor extractor = AiServices.builder(TopicExtractor.class)
                .chatModel(chatModel)
                .build();

        extractor.extract("All about climate change policy.");

        assertThat(chatModel.lastUserMessage()).contains("climate change policy");
    }
}
