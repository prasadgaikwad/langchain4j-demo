package dev.prasadgaikwad.langchain4jdemo.streaming;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Streams a single chat request through the {@link StreamingChatModel},
 * forwarding each token to a {@link StreamConsumer}. Used by both the SSE
 * endpoint and the WebSocket handler, and kept dependency-free of the web layer
 * so it can be unit-tested with a fake streaming model.
 */
@Service
public class ChatStreamingService {

    private final StreamingChatModel streamingChatModel;

    public ChatStreamingService(StreamingChatModel streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
    }

    public void stream(String message, StreamConsumer consumer) {
        streamingChatModel.chat(ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from("You are a helpful assistant. Answer concisely, in a few sentences."),
                        UserMessage.from(message)))
                .build(), new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                consumer.onToken(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                consumer.onComplete(completeResponse.aiMessage().text());
            }

            @Override
            public void onError(Throwable error) {
                consumer.onError(error);
            }
        });
    }

    /**
     * Callback contract for consuming streamed tokens.
     */
    public interface StreamConsumer {
        void onToken(String token);

        void onComplete(String fullText);

        void onError(Throwable error);
    }
}
