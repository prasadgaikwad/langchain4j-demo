package dev.prasadgaikwad.langchain4jdemo.multimodal;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisionServiceTest {

    private static final String CANNED_ANSWER = "A black cat sitting on a windowsill.";

    @Test
    void describeImageByUrlSendsTextAndImageContent() {
        FakeChatModel chatModel = new FakeChatModel(CANNED_ANSWER);
        VisionService visionService = new VisionService(chatModel);

        String answer = visionService.describeImage("https://example.com/cat.png", "What is in this image?");

        assertThat(answer).isEqualTo(CANNED_ANSWER);

        UserMessage userMessage = lastUserMessage(chatModel);
        assertThat(userMessage.contents()).hasSize(2);
        assertThat(userMessage.contents().get(0)).isInstanceOf(TextContent.class)
                .extracting(content -> ((TextContent) content).text())
                .isEqualTo("What is in this image?");
        ImageContent image = (ImageContent) userMessage.contents().get(1);
        assertThat(image.image().url().toString()).isEqualTo("https://example.com/cat.png");

        assertThat(chatModel.lastSystemMessage()).contains("vision assistant");
    }

    @Test
    void describeImageByBytesSendsBase64DataWithMimeType() {
        FakeChatModel chatModel = new FakeChatModel(CANNED_ANSWER);
        VisionService visionService = new VisionService(chatModel);
        byte[] pngBytes = new byte[] {1, 2, 3, 4};

        visionService.describeImage(pngBytes, "image/png", "What color is it?");

        ImageContent image = (ImageContent) lastUserMessage(chatModel).contents().get(1);
        assertThat(image.image().base64Data()).isNotBlank();
        assertThat(image.image().mimeType()).isEqualTo("image/png");
    }

    private static UserMessage lastUserMessage(FakeChatModel chatModel) {
        List<ChatMessage> messages = chatModel.lastMessages();
        return messages.stream()
                .filter(message -> message.type() == ChatMessageType.USER)
                .map(message -> (UserMessage) message)
                .findFirst()
                .orElseThrow();
    }
}
