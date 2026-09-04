package dev.prasadgaikwad.langchain4jdemo.api;

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
import dev.prasadgaikwad.langchain4jdemo.orchestration.StatefulPipelineService.StateEntry;
import dev.prasadgaikwad.langchain4jdemo.orchestration.HumanInTheLoopService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.HumanInTheLoopService.HitlResult;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cli.enabled=false")
@AutoConfigureMockMvc
class ChatApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Assistant assistant;

    @MockitoBean
    QaService qaService;

    @MockitoBean
    ChainService chainService;

    @MockitoBean
    ChatStreamingService streamingService;

    @MockitoBean
    ChainOfAgentsService chainOfAgentsService;

    @MockitoBean
    GraphOfAgentsService graphOfAgentsService;

    @MockitoBean
    WorkflowOfAgentsService workflowOfAgentsService;

    @MockitoBean
    ReactAgentService reactAgentService;

    @MockitoBean
    StatefulPipelineService statefulPipelineService;

    @MockitoBean
    HumanInTheLoopService humanInTheLoopService;

    @Autowired
    ConversationHistoryService historyService;

    @Test
    void chatReturnsTheAssistantAnswer() throws Exception {
        when(assistant.chat(anyString(), anyString())).thenReturn("Hello from the API!");

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hello from the API!"));
    }

    @Test
    void chatPersistsTheConversationHistory() throws Exception {
        when(assistant.chat(anyString(), anyString())).thenReturn("Answer one");

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"conversationId\":\"hist-chat\",\"message\":\"hello db\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/history/hist-chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].text").value("hello db"))
                .andExpect(jsonPath("$[1].role").value("ai"))
                .andExpect(jsonPath("$[1].text").value("Answer one"));
    }

    @Test
    void chatUsesTheProvidedConversationId() throws Exception {
        when(assistant.chat(anyString(), anyString())).thenReturn("ok");

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"conversationId\":\"web\",\"message\":\"hi\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(assistant).chat("web", "hi");
    }

    @Test
    void askReturnsTheRagAnswer() throws Exception {
        when(qaService.ask(anyString(), anyString())).thenReturn("Answer from documents");

        mockMvc.perform(post("/api/ask")
                        .contentType("application/json")
                        .content("{\"conversationId\":\"hist-ask\",\"message\":\"what is RAG?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Answer from documents"));

        mockMvc.perform(get("/api/history/hist-ask"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].role").value("ai"))
                .andExpect(jsonPath("$[1].text").value("Answer from documents"));
    }

    @Test
    void agentRunsTheTaskThroughTheChain() throws Exception {
        when(chainService.ask(anyString(), anyString())).thenReturn("Task done");

        mockMvc.perform(post("/api/agent")
                        .contentType("application/json")
                        .content("{\"conversationId\":\"hist-agent\",\"message\":\"summarize\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Task done"));

        mockMvc.perform(get("/api/history/hist-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("summarize"))
                .andExpect(jsonPath("$[1].text").value("Task done"));
    }

    @Test
    void streamEmitsTokensAsServerSentEvents() throws Exception {
        doAnswer(invocation -> {
            ChatStreamingService.StreamConsumer consumer = invocation.getArgument(1);
            consumer.onToken("Why");
            consumer.onToken(" ");
            consumer.onToken("not");
            consumer.onComplete("Why not");
            return null;
        }).when(streamingService).stream(anyString(), any());

        MvcResult result = mockMvc.perform(get("/api/chat/stream").param("message", "hi"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(content().string(containsString("\"Why\"")))
                .andExpect(content().string(containsString("\" \"")))
                .andExpect(content().string(containsString("\"not\"")));

        mockMvc.perform(get("/api/history/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("hi"))
                .andExpect(jsonPath("$[1].text").value("Why not"));
    }

    @Test
    void streamRecordsHistoryOnError() throws Exception {
        doAnswer(invocation -> {
            ChatStreamingService.StreamConsumer consumer = invocation.getArgument(1);
            consumer.onToken("Hello ");
            consumer.onToken("world");
            consumer.onError(new RuntimeException("stream failed"));
            return null;
        }).when(streamingService).stream(anyString(), any());

        mockMvc.perform(get("/api/chat/stream")
                        .param("message", "hi")
                        .param("conversationId", "error-stream"))
                .andReturn();

        mockMvc.perform(get("/api/history/error-stream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("hi"))
                .andExpect(jsonPath("$[1].text").value("Hello world"));
    }

    @Test
    void chainReturnsFullPipelineTrace() throws Exception {
        when(chainOfAgentsService.runWithTrace("test topic")).thenReturn(
                new ChainPipelineResult("test topic", "# Outline", "Draft text", "Edited text", "# Formatted post"));

        mockMvc.perform(post("/api/chain")
                        .contentType("application/json")
                        .content("{\"message\":\"test topic\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("test topic"))
                .andExpect(jsonPath("$.outline").value("# Outline"))
                .andExpect(jsonPath("$.draft").value("Draft text"))
                .andExpect(jsonPath("$.edited").value("Edited text"))
                .andExpect(jsonPath("$.formatted").value("# Formatted post"));
    }

    @Test
    void graphReturnsFullPipelineTrace() throws Exception {
        when(graphOfAgentsService.runWithTrace("test prompt")).thenReturn(
                new GraphPipelineResult("test prompt", "DevOps profile", "K8s topic",
                        "# Outline", "Draft text", "Edited text", "# Final writeup",
                        List.of("extractProfile", "suggestTopic", "createOutline",
                                "writeDraft", "editDraft", "createWriteup")));

        mockMvc.perform(post("/api/graph")
                        .contentType("application/json")
                        .content("{\"message\":\"test prompt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("test prompt"))
                .andExpect(jsonPath("$.profile").value("DevOps profile"))
                .andExpect(jsonPath("$.topic").value("K8s topic"))
                .andExpect(jsonPath("$.outline").value("# Outline"))
                .andExpect(jsonPath("$.draft").value("Draft text"))
                .andExpect(jsonPath("$.edited").value("Edited text"))
                .andExpect(jsonPath("$.writeup").value("# Final writeup"))
                .andExpect(jsonPath("$.agentPath").isArray())
                .andExpect(jsonPath("$.agentPath.length()").value(6));
    }

    @Test
    void workflowReturnsFullPipelineTrace() throws Exception {
        when(workflowOfAgentsService.run("test topic")).thenReturn(
                new WorkflowPipelineResult("test topic", "Combined research", "Draft text",
                        "# Formatted post", 2, "technical",
                        List.of("ResearchAgent1", "ResearchAgent2", "WorkflowDraftAgent",
                                "QualityScorerAgent", "ImproveAgent", "CategoryAgent",
                                "TechnicalFormatAgent")));

        mockMvc.perform(post("/api/workflow")
                        .contentType("application/json")
                        .content("{\"message\":\"test topic\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("test topic"))
                .andExpect(jsonPath("$.research").value("Combined research"))
                .andExpect(jsonPath("$.draft").value("Draft text"))
                .andExpect(jsonPath("$.formatted").value("# Formatted post"))
                .andExpect(jsonPath("$.refinementIterations").value(2))
                .andExpect(jsonPath("$.category").value("technical"))
                .andExpect(jsonPath("$.executedAgents").isArray())
                .andExpect(jsonPath("$.executedAgents.length()").value(7));
    }

    @Test
    void reactReturnsAgentResultWithSteps() throws Exception {
        when(reactAgentService.run("compute 2+2")).thenReturn(
                new ReactResult("compute 2+2", "The answer is 4.",
                        List.of("agent", "action", "agent"),
                        List.of("I need to calculate 2+2.", "4.0", "The answer is 4.")));

        mockMvc.perform(post("/api/react")
                        .contentType("application/json")
                        .content("{\"message\":\"compute 2+2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task").value("compute 2+2"))
                .andExpect(jsonPath("$.answer").value("The answer is 4."))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps.length()").value(3))
                .andExpect(jsonPath("$.agentTrace").isArray())
                .andExpect(jsonPath("$.agentTrace.length()").value(3));
    }

    @Test
    void statefulReactReturnsCheckpointHistory() throws Exception {
        when(statefulPipelineService.run("api", "compute 2+2")).thenReturn(
                new StatefulResult("session-abc", "compute 2+2", "The answer is 4.",
                        List.of("agent", "action", "agent"), 3,
                        List.of(new StateEntry("session-abc", "agent", "Thinking...", 1),
                                new StateEntry("session-abc", "action", "4.0", 2),
                                new StateEntry("session-abc", "__END__", "The answer is 4.", 3))));

        mockMvc.perform(post("/api/stateful/react")
                        .contentType("application/json")
                        .content("{\"message\":\"compute 2+2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-abc"))
                .andExpect(jsonPath("$.task").value("compute 2+2"))
                .andExpect(jsonPath("$.answer").value("The answer is 4."))
                .andExpect(jsonPath("$.checkpointCount").value(3))
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history.length()").value(3));
    }

    @Test
    void hitlStartReturnsAwaitingApprovalWhenPaused() throws Exception {
        when(humanInTheLoopService.start("api", "check the weather")).thenReturn(
                new HitlResult("session-abc", "check the weather", "",
                        List.of("agent"), true, "I want to call weather_tool(Tokyo)", null));

        mockMvc.perform(post("/api/hitl/react")
                        .contentType("application/json")
                        .content("{\"message\":\"check the weather\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-abc"))
                .andExpect(jsonPath("$.awaitingApproval").value(true))
                .andExpect(jsonPath("$.proposedAction").value("I want to call weather_tool(Tokyo)"))
                .andExpect(jsonPath("$.steps.length()").value(1));
    }

    @Test
    void hitlResumeReturnsFinalAnswerAfterApproval() throws Exception {
        when(humanInTheLoopService.resume("session-abc", true, null)).thenReturn(
                new HitlResult("session-abc", "(approved)", "The weather in Tokyo is 22C.",
                        List.of("agent", "action", "agent"), false, "", ""));

        mockMvc.perform(post("/api/hitl/react/resume")
                        .contentType("application/json")
                        .content("{\"sessionId\":\"session-abc\",\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awaitingApproval").value(false))
                .andExpect(jsonPath("$.answer").value("The weather in Tokyo is 22C."))
                .andExpect(jsonPath("$.steps.length()").value(3));
    }

    @Test
    void streamRecordsUnderTheProvidedConversationId() throws Exception {
        doAnswer(invocation -> {
            ChatStreamingService.StreamConsumer consumer = invocation.getArgument(1);
            consumer.onToken("ok");
            consumer.onComplete("ok");
            return null;
        }).when(streamingService).stream(anyString(), any());

        MvcResult result = mockMvc.perform(get("/api/chat/stream")
                        .param("message", "hello")
                        .param("conversationId", "hist-stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/history/hist-stream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("hello"))
                .andExpect(jsonPath("$[1].text").value("ok"));
    }
}
