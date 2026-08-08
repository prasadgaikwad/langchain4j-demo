package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cli.enabled=false")
@AutoConfigureMockMvc
class HistoryApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ConversationHistoryService historyService;

    @Test
    void recordsAndFetchesHistoryEndToEnd() throws Exception {
        historyService.record("api-history", "user", "hello db");
        historyService.record("api-history", "ai", "hi from db");

        mockMvc.perform(get("/api/history/api-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].text").value("hello db"))
                .andExpect(jsonPath("$[1].role").value("ai"))
                .andExpect(jsonPath("$[1].text").value("hi from db"));

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*]").value(org.hamcrest.Matchers.hasItem("api-history")));
    }

    @Test
    void returnsNotFoundForUnknownConversation() throws Exception {
        mockMvc.perform(get("/api/history/no-such-conversation"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearingHistoryRemovesTheConversation() throws Exception {
        historyService.record("to-clear", "user", "temp");

        mockMvc.perform(delete("/api/history/to-clear"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/history/to-clear"))
                .andExpect(status().isNotFound());
    }
}
