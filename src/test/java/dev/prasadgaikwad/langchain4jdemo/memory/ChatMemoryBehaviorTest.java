package dev.prasadgaikwad.langchain4jdemo.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryBehaviorTest {

    @Test
    void messageWindowKeepsRecentMessagesAndEvictsOldest() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(3);

        memory.add(UserMessage.from("first"));
        memory.add(AiMessage.from("answer 1"));
        memory.add(UserMessage.from("second"));
        memory.add(AiMessage.from("answer 2"));

        assertThat(userTexts(memory.messages())).containsExactly("second");
    }

    private static List<String> userTexts(List<ChatMessage> messages) {
        return messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(message -> ((UserMessage) message).singleText())
                .toList();
    }

    @Test
    void tokenWindowEvictsOldestMessagesBeyondTokenBudget() {
        OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator("gpt-4o-mini");
        TokenWindowChatMemory memory = TokenWindowChatMemory.builder()
                .maxTokens(15, estimator)
                .build();

        memory.add(UserMessage.from("hello"));
        memory.add(AiMessage.from("hi there, how can I help you today?"));
        memory.add(UserMessage.from("do you remember my name?"));

        assertThat(userTexts(memory.messages())).containsExactly("do you remember my name?");
        assertThat(estimator.estimateTokenCountInMessages(memory.messages())).isLessThanOrEqualTo(15);
    }

    @Test
    void clearRemovesAllMessages() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
        memory.add(UserMessage.from("hello"));

        memory.clear();

        assertThat(memory.messages()).isEmpty();
    }
}
