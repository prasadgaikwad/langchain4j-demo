package dev.prasadgaikwad.langchain4jdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cli.enabled=false")
@AutoConfigureMockMvc
class DevToolingTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void actuatorHealthReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorExposesInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("langchain4j-demo"));
    }

    @Test
    void openApiDocsDescribeTheRestApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/chat")))
                .andExpect(content().string(containsString("/api/ask")))
                .andExpect(content().string(containsString("/api/chat/stream")));
    }

    @Test
    void openApiDocsIncludeOperationAndSchemaDescriptions() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("LangChain4j Demo API"))
                .andExpect(jsonPath("$.tags[*].name", org.hamcrest.Matchers.hasItem("Chat")))
                .andExpect(jsonPath("$.tags[*].name", org.hamcrest.Matchers.hasItem("Prompting")))
                .andExpect(jsonPath("$.paths['/api/chat'].post.summary")
                        .value("Chat with the memory-backed assistant"))
                .andExpect(jsonPath("$.paths['/api/chat/stream'].get.parameters[0].name")
                        .value("message"))
                .andExpect(jsonPath("$.components.schemas.ChatRequest.properties.message.description")
                        .value("The user message or task to run"));
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
