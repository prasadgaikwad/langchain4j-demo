package dev.prasadgaikwad.langchain4jdemo.ws;

import tools.jackson.databind.json.JsonMapper;
import dev.prasadgaikwad.langchain4jdemo.FakeStreamingChatModel;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketHandlerTest {

    private final ChatStreamingService streamingService =
            new ChatStreamingService(new FakeStreamingChatModel("hel", "lo"));
    private final ChatWebSocketHandler handler =
            new ChatWebSocketHandler(streamingService, new JsonMapper());

    @Test
    void streamsTokensAndDoneMarkerAsFrames() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage("{\"message\":\"hi\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(3)).sendMessage(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(TextMessage::getPayload)
                .containsExactly("hel", "lo", "[DONE]");
    }

    @Test
    void reportsInvalidPayloadsWithoutStreaming() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage("not-json"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).startsWith("[ERROR]");
    }
}
