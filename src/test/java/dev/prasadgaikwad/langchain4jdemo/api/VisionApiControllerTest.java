package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.multimodal.VisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cli.enabled=false")
@AutoConfigureMockMvc
class VisionApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    VisionService visionService;

    @Test
    void describeWithUrlReturnsTheAnswer() throws Exception {
        when(visionService.describeImage("https://example.com/cat.png", "What is this?"))
                .thenReturn("A black cat.");

        mockMvc.perform(post("/api/describe")
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"https://example.com/cat.png\",\"question\":\"What is this?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A black cat."));
    }

    @Test
    void describeWithoutImageReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/describe")
                        .contentType("application/json")
                        .content("{\"question\":\"What is this?\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void describeWithMalformedBase64ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/describe")
                        .contentType("application/json")
                        .content("""
                                {"imageData":"not-base64-data!!","mimeType":"image/png","question":"What is this?"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void describeWithMalformedImageUrlReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/describe")
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"ht tp://bad url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void describeWithRelativeImageUrlReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/describe")
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"example.com/cat.png\"}"))
                .andExpect(status().isBadRequest());
    }
}
