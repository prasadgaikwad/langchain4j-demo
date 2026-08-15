package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.List;

/**
 * Deterministic streaming chat model used to avoid real API calls in tests.
 * Emits the configured tokens through the handler and then completes.
 */
public class FakeStreamingChatModel implements StreamingChatModel {

    private final List<String> tokens;
    private ChatRequest lastRequest;

    public FakeStreamingChatModel(String... tokens) {
        this.tokens = List.of(tokens);
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        this.lastRequest = chatRequest;
        StringBuilder fullText = new StringBuilder();
        for (String token : tokens) {
            handler.onPartialResponse(token);
            fullText.append(token);
        }
        handler.onCompleteResponse(ChatResponse.builder()
                .aiMessage(AiMessage.from(fullText.toString()))
                .build());
    }

    public ChatRequest lastRequest() {
        return lastRequest;
    }

    public List<String> lastRequestToolNames() {
        return lastRequest == null || lastRequest.toolSpecifications() == null
                ? List.of()
                : lastRequest.toolSpecifications().stream().map(spec -> spec.name()).toList();
    }
}
