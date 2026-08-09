package dev.prasadgaikwad.langchain4jdemo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Chat", description = "Chat, RAG, and agent endpoints backed by LangChain4j AI services")
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
    @Operation(summary = "Chat with the memory-backed assistant",
            description = "Sends the message to the assistant with conversation memory. "
                    + "Use a conversationId to keep a multi-turn conversation; a new id starts fresh.")
    @ApiResponse(responseCode = "200", description = "The assistant's answer",
            content = @Content(schema = @Schema(implementation = ChatResponse.class)))
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = assistant.chat(request.conversationId(), request.message());
        return new ChatResponse(answer);
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question over indexed documents (RAG)",
            description = "Answers the question using the most relevant chunks from the "
                    + "embedding store via a RetrievalAugmentor.")
    @ApiResponse(responseCode = "200", description = "The answer grounded in the indexed documents",
            content = @Content(schema = @Schema(implementation = ChatResponse.class)))
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String answer = qaService.ask(request.conversationId(), request.message());
        return new ChatResponse(answer);
    }

    @PostMapping("/agent")
    @Operation(summary = "Delegate a task to the tool-using agent",
            description = "Runs the task through the custom chain, routing arithmetic to a local "
                    + "calculator and everything else to the tool-using agent (search, store stats).")
    @ApiResponse(responseCode = "200", description = "The agent's result",
            content = @Content(schema = @Schema(implementation = ChatResponse.class)))
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
    @Operation(summary = "Stream a chat reply token by token (SSE)",
            description = "Returns a text/event-stream where every event is a JSON-encoded token. "
                    + "Each token is JSON-encoded so spaces between words survive the SSE transport.")
    @ApiResponse(responseCode = "200", description = "A stream of JSON-encoded tokens")
    public SseEmitter stream(@Parameter(description = "The user message to stream", example = "Tell me a short joke")
                             @RequestParam String message) {
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
