package dev.prasadgaikwad.langchain4jdemo.ws;

import tools.jackson.databind.json.JsonMapper;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;

import java.io.IOException;

/**
 * WebSocket chat endpoint: the client sends a JSON message and receives the
 * chat reply as one text frame per token, ending with a {@code [DONE]} frame.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatStreamingService streamingService;
    private final JsonMapper objectMapper;

    public ChatWebSocketHandler(ChatStreamingService streamingService, JsonMapper objectMapper) {
        this.streamingService = streamingService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ChatMessagePayload payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), ChatMessagePayload.class);
        } catch (JacksonException e) {
            sendQuietly(session, new TextMessage("[ERROR] Invalid message payload: " + e.getMessage()));
            return;
        }

        streamingService.stream(payload.message(), new ChatStreamingService.StreamConsumer() {
            @Override
            public void onToken(String token) {
                sendQuietly(session, new TextMessage(token));
            }

            @Override
            public void onComplete(String fullText) {
                sendQuietly(session, new TextMessage("[DONE]"));
            }

            @Override
            public void onError(Throwable error) {
                sendQuietly(session, new TextMessage("[ERROR] " + error.getMessage()));
            }
        });
    }

    private static void sendQuietly(WebSocketSession session, TextMessage textMessage) {
        try {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        } catch (IOException e) {
            // client gone; nothing to do
        }
    }
}
