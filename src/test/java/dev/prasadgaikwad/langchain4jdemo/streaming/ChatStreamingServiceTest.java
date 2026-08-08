package dev.prasadgaikwad.langchain4jdemo.streaming;

import dev.prasadgaikwad.langchain4jdemo.FakeStreamingChatModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStreamingServiceTest {

    @Test
    void forwardsEachTokenThenCompletes() {
        ChatStreamingService service = new ChatStreamingService(
                new FakeStreamingChatModel("Hel", "lo", " ", "world"));

        List<String> tokens = new ArrayList<>();
        List<String> completions = new ArrayList<>();
        service.stream("hi", new ChatStreamingService.StreamConsumer() {
            @Override
            public void onToken(String token) {
                tokens.add(token);
            }

            @Override
            public void onComplete(String fullText) {
                completions.add(fullText);
            }

            @Override
            public void onError(Throwable error) {
                throw new RuntimeException(error);
            }
        });

        assertThat(tokens).containsExactly("Hel", "lo", " ", "world");
        assertThat(completions).containsExactly("Hello world");
    }

    @Test
    void singleTokenStreamCompletesWithThatToken() {
        ChatStreamingService service = new ChatStreamingService(new FakeStreamingChatModel("ping"));

        List<String> completions = new ArrayList<>();
        service.stream("x", new ChatStreamingService.StreamConsumer() {
            @Override
            public void onToken(String token) {
            }

            @Override
            public void onComplete(String fullText) {
                completions.add(fullText);
            }

            @Override
            public void onError(Throwable error) {
                throw new RuntimeException(error);
            }
        });

        assertThat(completions).containsExactly("ping");
    }
}
