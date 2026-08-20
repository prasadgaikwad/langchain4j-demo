package dev.prasadgaikwad.langchain4jdemo.api;

import tools.jackson.databind.json.JsonMapper;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ChainOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ChainPipelineResult;
import dev.prasadgaikwad.langchain4jdemo.orchestration.GraphOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.GraphPipelineResult;
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
    private final ConversationHistoryService historyService;
    private final JsonMapper objectMapper;
    private final ChainOfAgentsService chainOfAgentsService;
    private final GraphOfAgentsService graphOfAgentsService;

    public ChatApiController(Assistant assistant,
                             QaService qaService,
                             ChainService chainService,
                             ChatStreamingService streamingService,
                             ConversationHistoryService historyService,
                             JsonMapper objectMapper,
                             ChainOfAgentsService chainOfAgentsService,
                             GraphOfAgentsService graphOfAgentsService) {
        this.assistant = assistant;
        this.qaService = qaService;
        this.chainService = chainService;
        this.streamingService = streamingService;
        this.historyService = historyService;
        this.objectMapper = objectMapper;
        this.chainOfAgentsService = chainOfAgentsService;
        this.graphOfAgentsService = graphOfAgentsService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with the memory-backed assistant",
            description = "Sends the message to the assistant with conversation memory. "
                    + "Use a conversationId to keep a multi-turn conversation; a new id starts fresh. "
                    + "Each turn is persisted to the conversation history.")
    @ApiResponse(responseCode = "200", description = "The assistant's answer",
            content = @Content(schema = @Schema(implementation = ChatResponse.class)))
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = assistant.chat(request.conversationId(), request.message());
        recordTurn(request.conversationId(), request.message(), answer);
        return new ChatResponse(answer);
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question over indexed documents (RAG)",
            description = "Answers the question using the most relevant chunks from the "
                    + "embedding store via a RetrievalAugmentor. The question and answer are "
                    + "persisted to the conversation history.")
    @ApiResponse(responseCode = "200", description = "The answer grounded in the indexed documents",
            content = @Content(schema = @Schema(implementation = ChatResponse.class)))
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String answer = qaService.ask(request.conversationId(), request.message());
        recordTurn(request.conversationId(), request.message(), answer);
        return new ChatResponse(answer);
    }

    @PostMapping("/agent")
    @Operation(summary = "Delegate a task to the tool-using agent",
            description = "Runs the task through the custom chain, routing arithmetic to a local "
                    + "calculator and everything else to the tool-using agent (search, store stats). "
                    + "The task and result are persisted to the conversation history.")
    @ApiResponse(responseCode = "200", description = "The agent's result",
            content = @Content(schema = @Schema(implementation = ChatResponse.class)))
    public ChatResponse agent(@RequestBody ChatRequest request) {
        String answer = chainService.ask(request.conversationId(), request.message());
        recordTurn(request.conversationId(), request.message(), answer);
        return new ChatResponse(answer);
    }

    @PostMapping("/chain")
    @Operation(summary = "Generate a blog post via a sequential chain of agents",
            description = "Runs the topic through a pipeline of Outline -> Draft -> Edit -> Format agents. "
                    + "Returns the full pipeline trace with each intermediate stage.")
    @ApiResponse(responseCode = "200", description = "The complete pipeline trace",
            content = @Content(schema = @Schema(implementation = ChainResponse.class)))
    public ChainResponse chain(@RequestBody ChatRequest request) {
        ChainPipelineResult result = chainOfAgentsService.runWithTrace(request.message());
        return new ChainResponse(
                result.topic(),
                result.outline(),
                result.draft(),
                result.edited(),
                result.formatted());
    }

    @PostMapping("/graph")
    @Operation(summary = "Generate a personalized blog post via a goal-oriented agent graph (GOAP)",
            description = "Runs the prompt through a GOAP-planned pipeline of agents. "
                    + "The planner analyzes agent dependencies and computes the shortest execution path. "
                    + "Returns the full trace including the computed agent path.")
    @ApiResponse(responseCode = "200", description = "The complete graph pipeline trace",
            content = @Content(schema = @Schema(implementation = GraphResponse.class)))
    public GraphResponse graph(@RequestBody ChatRequest request) {
        GraphPipelineResult result = graphOfAgentsService.runWithTrace(request.message());
        return new GraphResponse(
                result.prompt(),
                result.profile(),
                result.topic(),
                result.outline(),
                result.draft(),
                result.edited(),
                result.writeup(),
                result.agentPath());
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
                    + "Each token is JSON-encoded so spaces between words survive the SSE transport. "
                    + "The message and the complete reply are persisted to the conversation history.")
    @ApiResponse(responseCode = "200", description = "A stream of JSON-encoded tokens")
    public SseEmitter stream(@Parameter(description = "The user message to stream", example = "Tell me a short joke")
                             @RequestParam String message,
            @Parameter(description = "Conversation (memory) id", example = "web",
                    schema = @Schema(defaultValue = "api"))
            @RequestParam(defaultValue = "api") String conversationId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        streamingService.stream(message, new ChatStreamingService.StreamConsumer() {
            @Override
            public void onToken(String token) {
                send(emitter, token);
            }

            @Override
            public void onComplete(String fullText) {
                recordTurn(conversationId, message, fullText);
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        });
        return emitter;
    }

    private void recordTurn(String conversationId, String userMessage, String aiAnswer) {
        historyService.record(conversationId, "user", userMessage);
        historyService.record(conversationId, "ai", aiAnswer);
    }

    private void send(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(token)));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
