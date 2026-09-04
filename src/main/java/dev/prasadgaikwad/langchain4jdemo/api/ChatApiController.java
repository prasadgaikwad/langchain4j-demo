package dev.prasadgaikwad.langchain4jdemo.api;

import tools.jackson.databind.json.JsonMapper;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ChainOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ChainPipelineResult;
import dev.prasadgaikwad.langchain4jdemo.orchestration.GraphOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.GraphPipelineResult;
import dev.prasadgaikwad.langchain4jdemo.orchestration.WorkflowOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.WorkflowPipelineResult;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ReactAgentService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ReactAgentService.ReactResult;
import dev.prasadgaikwad.langchain4jdemo.orchestration.StatefulPipelineService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.StatefulPipelineService.StatefulResult;
import dev.prasadgaikwad.langchain4jdemo.orchestration.HumanInTheLoopService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.HumanInTheLoopService.HitlResult;
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
import java.util.ArrayList;
import java.util.List;

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
    private final WorkflowOfAgentsService workflowOfAgentsService;
    private final ReactAgentService reactAgentService;
    private final StatefulPipelineService statefulPipelineService;
    private final HumanInTheLoopService humanInTheLoopService;

    public ChatApiController(Assistant assistant,
                             QaService qaService,
                             ChainService chainService,
                             ChatStreamingService streamingService,
                             ConversationHistoryService historyService,
                             JsonMapper objectMapper,
                             ChainOfAgentsService chainOfAgentsService,
                             GraphOfAgentsService graphOfAgentsService,
                             WorkflowOfAgentsService workflowOfAgentsService,
                             ReactAgentService reactAgentService,
                             StatefulPipelineService statefulPipelineService,
                             HumanInTheLoopService humanInTheLoopService) {
        this.assistant = assistant;
        this.qaService = qaService;
        this.chainService = chainService;
        this.streamingService = streamingService;
        this.historyService = historyService;
        this.objectMapper = objectMapper;
        this.chainOfAgentsService = chainOfAgentsService;
        this.graphOfAgentsService = graphOfAgentsService;
        this.workflowOfAgentsService = workflowOfAgentsService;
        this.reactAgentService = reactAgentService;
        this.statefulPipelineService = statefulPipelineService;
        this.humanInTheLoopService = humanInTheLoopService;
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

    @PostMapping("/workflow")
    @Operation(summary = "Generate a blog post via a workflow pipeline with parallel research, iterative refinement, and conditional formatting",
            description = "Runs the topic through a workflow combining parallel research, draft creation, "
                    + "iterative quality refinement (loop with exit condition), and conditional formatting. "
                    + "Returns the full pipeline trace including refinement iterations and category.")
    @ApiResponse(responseCode = "200", description = "The complete workflow pipeline trace",
            content = @Content(schema = @Schema(implementation = WorkflowResponse.class)))
    public WorkflowResponse workflow(@RequestBody ChatRequest request) {
        WorkflowPipelineResult result = workflowOfAgentsService.run(request.message());
        return new WorkflowResponse(
                result.topic(),
                result.research(),
                result.draft(),
                result.formatted(),
                result.refinementIterations(),
                result.category(),
                result.executedAgents());
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

    @PostMapping("/react")
    @Operation(summary = "Run a task through the LangGraph4j ReACT agent executor",
            description = "Runs the task through an explicit agent→action→agent state graph built with "
                    + "LangGraph4j's AgentExecutor. The LLM reasons, selects tools, observes results, "
                    + "and iterates until it produces a final answer. Returns the graph step trace and answer.")
    @ApiResponse(responseCode = "200", description = "The ReACT agent result with step trace",
            content = @Content(schema = @Schema(implementation = ReactResponse.class)))
    public ReactResponse react(@RequestBody ChatRequest request) {
        ReactResult result = reactAgentService.run(request.message());
        return new ReactResponse(result.task(), result.answer(), result.steps(), result.agentTrace());
    }

    @PostMapping("/stateful/react")
    @Operation(summary = "Run a task through the LangGraph4j ReACT agent with checkpoint persistence",
            description = "Like /react, but checkpoints the graph state after each run using a thread-based "
                    + "MemorySaver. Pass conversationId as the session ID to resume; omit to start a new session. "
                    + "Returns the step trace, answer, and full checkpoint history for the session.")
    @ApiResponse(responseCode = "200", description = "The stateful pipeline result with checkpoint history",
            content = @Content(schema = @Schema(implementation = StatefulReactResponse.class)))
    public StatefulReactResponse statefulReact(@RequestBody ChatRequest request) {
        StatefulPipelineService.StatefulResult result = statefulPipelineService.run(
                request.conversationId(), request.message());
        List<StatefulReactResponse.StateEntry> historyEntries = result.history().stream()
                .map(h -> new StatefulReactResponse.StateEntry(h.threadId(), h.node(), h.lastAiMessage(), h.messageCount()))
                .toList();
        return new StatefulReactResponse(
                result.sessionId(),
                result.task(),
                result.answer(),
                result.steps(),
                result.checkpointCount(),
                historyEntries);
    }

    @PostMapping("/hitl/react")
    @Operation(summary = "Start a human-in-the-loop ReACT run",
            description = "Runs the task through the LangGraph4j AgentExecutor with an interrupt before every "
                    + "tool execution. When the agent proposes a tool call, the graph pauses and returns "
                    + "awaitingApproval=true with the proposed action. Review it, then POST /api/hitl/react/resume.")
    @ApiResponse(responseCode = "200", description = "The HITL result (may be awaiting approval)",
            content = @Content(schema = @Schema(implementation = HitlResponse.class)))
    public HitlResponse hitlStart(@RequestBody ChatRequest request) {
        HitlResult result = humanInTheLoopService.start(request.conversationId(), request.message());
        return toHitlResponse(result);
    }

    @PostMapping("/hitl/react/resume")
    @Operation(summary = "Resume (approve or reject) a paused human-in-the-loop run",
            description = "Resumes a graph paused before a tool execution. Set approved=true to execute the "
                    + "proposed action, or false to reject; feedback is recorded either way. The run may pause "
                    + "again if the agent proposes another tool call.")
    @ApiResponse(responseCode = "200", description = "The HITL result after resuming",
            content = @Content(schema = @Schema(implementation = HitlResponse.class)))
    public HitlResponse hitlResume(@RequestBody HitlResumeRequest request) {
        HitlResult result = humanInTheLoopService.resume(
                request.sessionId(), request.approved(), request.feedback());
        return toHitlResponse(result);
    }

    private HitlResponse toHitlResponse(HitlResult result) {
        return new HitlResponse(result.sessionId(), result.task(), result.answer(), result.steps(),
                result.awaitingApproval(), result.proposedAction(), result.feedback());
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
        List<String> tokens = new ArrayList<>();
        streamingService.stream(message, new ChatStreamingService.StreamConsumer() {
            @Override
            public void onToken(String token) {
                send(emitter, token);
                tokens.add(token);
            }

            @Override
            public void onComplete(String fullText) {
                recordTurn(conversationId, message, fullText);
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                String partial = String.join("", tokens);
                if (!partial.isEmpty()) {
                    recordTurn(conversationId, message, partial);
                }
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
