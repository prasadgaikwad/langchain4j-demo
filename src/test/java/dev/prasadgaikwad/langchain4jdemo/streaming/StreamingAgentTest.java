package dev.prasadgaikwad.langchain4jdemo.streaming;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.FakeStreamingChatModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingAgentTest {

    @Test
    void streamsTokensThroughTheAiServiceProxy() {
        FakeStreamingChatModel model = new FakeStreamingChatModel("The ", "answer ", "is 4.");
        StreamingAgent agent = AiServices.builder(StreamingAgent.class)
                .streamingChatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(new CalculatorTool())
                .build();

        List<String> tokens = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        agent.chat("stream", "What is 2 + 2?")
                .onPartialResponse(tokens::add)
                .onCompleteResponse(response -> fullText.append(response.aiMessage().text()))
                .onError(errors::add)
                .start();

        assertThat(tokens).containsExactly("The ", "answer ", "is 4.");
        assertThat(fullText).hasToString("The answer is 4.");
        assertThat(errors).isEmpty();
        assertThat(model.lastRequestToolNames()).contains("calculate");
    }
}
