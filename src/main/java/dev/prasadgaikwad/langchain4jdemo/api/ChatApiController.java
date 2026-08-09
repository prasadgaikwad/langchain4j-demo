package dev.prasadgaikwad.langchain4jdemo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * REST endpoints for chat, RAG, and the tool-using agent, plus a Server-Sent
 * Events endpoint for streaming the chat reply token by token.
 */
@RestController
@RequestMapping("/api")
public class ChatApiController {

    private final Assistant assistant;
    private final QaService qaService;
    private final ChainService chainService;
    private final ChatStreamingService streamingService;
    private final ObjectMapper objectMapper;

    public ChatApiController(Assistant assistant,
                             QaService qaService,
                             ChainService chainService,
                             ChatStreamingService streamingService,
                             ObjectMapper objectMapper) {
        this.assistant = assistant;
        this.qaService = qaService;
        this.chainService = chainService;
        this.streamingService = streamingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = assistant.chat(request.conversationId(), request.message());
        return new ChatResponse(answer);
    }

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String answer = qaService.ask(request.conversationId(), request.message());
        return new ChatResponse(answer);
    }

    @PostMapping("/agent")
    public ChatResponse agent(@RequestBody ChatRequest request) {
        String answer = chainService.ask(request.conversationId(), request.message());
        return new ChatResponse(answer);
    }

    /**
     * Streams the chat reply as Server-Sent Events, one event per token. Each
     * token is sent JSON-encoded so whitespace survives: the browser strips a
     * leading space from a plain {@code data: } line, which would otherwise
     * silently drop spaces between words.
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(60_000L);
        streamingService.stream(message, new ChatStreamingService.StreamConsumer() {
            @Override
            public void onToken(String token) {
                send(emitter, token);
            }

            @Override
            public void onComplete(String fullText) {
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(token)));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
