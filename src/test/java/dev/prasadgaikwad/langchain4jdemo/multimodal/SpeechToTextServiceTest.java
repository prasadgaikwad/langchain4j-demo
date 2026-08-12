package dev.prasadgaikwad.langchain4jdemo.multimodal;

import dev.langchain4j.data.audio.Audio;
import dev.prasadgaikwad.langchain4jdemo.FakeAudioTranscriptionModel;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechToTextServiceTest {

    @Test
    void transcribeSendsBase64AudioAndReturnsTheText() {
        FakeAudioTranscriptionModel model = new FakeAudioTranscriptionModel();
        SpeechToTextService service = new SpeechToTextService(model);
        byte[] wavBytes = new byte[] {10, 20, 30};

        String text = service.transcribe(wavBytes, "audio/wav");

        assertThat(text).isEqualTo("Hello from the recording.");
        Audio audio = model.lastRequest().audio();
        assertThat(audio.base64Data()).isEqualTo(Base64.getEncoder().encodeToString(wavBytes));
        assertThat(audio.mimeType()).isEqualTo("audio/wav");
    }
}
