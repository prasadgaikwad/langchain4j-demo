package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import org.junit.jupiter.api.Test;
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
