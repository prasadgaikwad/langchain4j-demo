package dev.prasadgaikwad.langchain4jdemo.multimodal;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import org.springframework.stereotype.Service;

/**
 * Image-generation capability: turns a text prompt into an image using the
 * configured {@link ImageModel} (OpenAI's {@code gpt-image-1} by default).
 * <p>
 * The returned {@link Image} carries either a public URL or base64-encoded data
 * plus its mime type, depending on what the model returned.
 */
@Service
public class ImageGenerationService {

    private final ImageModel imageModel;

    public ImageGenerationService(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    /**
     * Generates a single image for the prompt.
     */
    public Image generate(String prompt) {
        return imageModel.generate(prompt).content();
    }
}
