package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic image model used to avoid real API calls in tests. Captures the
 * last prompt and returns a canned image with a URL.
 */
public class FakeImageModel implements ImageModel {

    private String lastPrompt;
    private int lastCount;

    @Override
    public Response<Image> generate(String prompt) {
        this.lastPrompt = prompt;
        this.lastCount = 1;
        return Response.from(image());
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        this.lastPrompt = prompt;
        this.lastCount = n;
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            images.add(image());
        }
        return Response.from(images);
    }

    public String lastPrompt() {
        return lastPrompt;
    }

    public int lastCount() {
        return lastCount;
    }

    private static Image image() {
        return Image.builder()
                .url(URI.create("https://example.com/generated.png"))
                .mimeType("image/png")
                .revisedPrompt("revised prompt")
                .build();
    }
}
