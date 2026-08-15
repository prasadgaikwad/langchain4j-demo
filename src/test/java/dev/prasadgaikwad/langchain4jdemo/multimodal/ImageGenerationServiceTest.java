package dev.prasadgaikwad.langchain4jdemo.multimodal;

import dev.langchain4j.data.image.Image;
import dev.prasadgaikwad.langchain4jdemo.FakeImageModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageGenerationServiceTest {

    @Test
    void generateReturnsTheModelImageForThePrompt() {
        FakeImageModel imageModel = new FakeImageModel();
        ImageGenerationService service = new ImageGenerationService(imageModel);

        Image image = service.generate("a robot painting a sunset");

        assertThat(image.url().toString()).isEqualTo("https://example.com/generated.png");
        assertThat(imageModel.lastPrompt()).isEqualTo("a robot painting a sunset");
        assertThat(imageModel.lastCount()).isEqualTo(1);
    }

    @Test
    void generateWithCountRequestsMultipleAndReturnsFirst() {
        FakeImageModel imageModel = new FakeImageModel();
        ImageGenerationService service = new ImageGenerationService(imageModel);

        Image image = service.generate("a fox in a forest", 3);

        assertThat(image.url()).isNotNull();
        assertThat(imageModel.lastCount()).isEqualTo(3);
    }
}
