package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

/**
 * Deterministic chat model used to avoid real API calls in tests.
 * <p>
 * Captures the generated {@link ChatRequest} so tests can assert on the prompt
 * that was actually built, and returns a canned {@link AiMessage} so tests can
 * verify how the reply is parsed into the AI Service's return type.
 */
public class FakeChatModel implements ChatModel {

    private final String responseText;
    private ChatRequest lastRequest;

    public FakeChatModel(String responseText) {
        this.responseText = responseText;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        this.lastRequest = chatRequest;
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(responseText))
                .build();
    }

    public ChatRequest lastRequest() {
        return lastRequest;
    }

    public List<ChatMessage> lastMessages() {
        return lastRequest.messages();
    }

    public String lastSystemMessage() {
        return lastMessages().stream()
                .filter(message -> message.type() == ChatMessageType.SYSTEM)
                .map(message -> ((SystemMessage) message).text())
                .findFirst()
                .orElse("");
    }

    public String lastUserMessage() {
        return lastMessages().stream()
                .filter(message -> message.type() == ChatMessageType.USER)
                .map(message -> ((UserMessage) message).singleText())
                .findFirst()
                .orElse("");
    }
}
