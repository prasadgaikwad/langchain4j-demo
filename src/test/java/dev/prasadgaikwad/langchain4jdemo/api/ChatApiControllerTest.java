package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.streaming.ChatStreamingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
                        .content("{\"message\":\"what is RAG?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Answer from documents"));
    }

    @Test
    void agentRunsTheTaskThroughTheChain() throws Exception {
        when(chainService.ask(anyString(), anyString())).thenReturn("Task done");

        mockMvc.perform(post("/api/agent")
                        .contentType("application/json")
                        .content("{\"message\":\"summarize\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Task done"));
    }

    @Test
    void streamEmitsTokensAsServerSentEvents() throws Exception {
        doAnswer(invocation -> {
            ChatStreamingService.StreamConsumer consumer = invocation.getArgument(1);
            consumer.onToken("Hel");
            consumer.onToken("lo");
            consumer.onComplete("Hello");
            return null;
        }).when(streamingService).stream(anyString(), any());

        MvcResult result = mockMvc.perform(get("/api/chat/stream").param("message", "hi"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(content().string(containsString("Hel")))
                .andExpect(content().string(containsString("lo")));
    }
}
