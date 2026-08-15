package dev.prasadgaikwad.langchain4jdemo.multimodal;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * Vision capability: sends an image together with a question to a multimodal
 * chat model. The image can be referenced by URL or supplied as raw bytes
 * (base64-encoded with a mime type), matching the two ways OpenAI accepts image
 * input. The chat model returns a text description of the image.
 * <p>
 * In tests the configured {@link ChatModel} is a fake, so the service runs
 * offline; tests assert that the image content actually reached the request.
 */
@Service
public class VisionService {

    private static final String SYSTEM_PROMPT = """
            You are a vision assistant. Describe the image you are shown and answer
            the user's question about it. Be specific about what is visible in the image.
            """;

    private final ChatModel chatModel;

    public VisionService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Describes the image at the given public URL.
     */
    public String describeImage(String imageUrl, String question) {
        return describe(ImageContent.from(URI.create(imageUrl)), question);
    }

    /**
     * Describes an image supplied as raw bytes, e.g. uploaded or local files.
     */
    public String describeImage(byte[] imageData, String mimeType, String question) {
        return describe(ImageContent.from(Base64.getEncoder().encodeToString(imageData), mimeType), question);
    }

    private String describe(ImageContent image, String question) {
        return chatModel.chat(ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(SYSTEM_PROMPT),
                        UserMessage.from(TextContent.from(question), image)))
                .build())
                .aiMessage().text();
    }
}
