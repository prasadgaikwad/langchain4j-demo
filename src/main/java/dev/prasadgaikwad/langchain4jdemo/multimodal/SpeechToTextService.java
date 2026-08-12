package dev.prasadgaikwad.langchain4jdemo.multimodal;

import dev.langchain4j.data.audio.Audio;
import dev.langchain4j.model.audio.AudioTranscriptionModel;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * Speech-to-text capability: transcribes audio (a recording, a voice memo) into
 * text using the configured {@link AudioTranscriptionModel} (OpenAI's
 * {@code whisper-1} by default).
 */
@Service
public class SpeechToTextService {

    private final AudioTranscriptionModel transcriptionModel;

    public SpeechToTextService(AudioTranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    /**
     * Transcribes the given audio bytes (WAV/MP3/OGG/M4A) into text.
     */
    public String transcribe(byte[] audioData, String mimeType) {
        Audio audio = Audio.builder()
                .base64Data(Base64.getEncoder().encodeToString(audioData))
                .mimeType(mimeType)
                .build();
        return transcriptionModel.transcribeToText(audio);
    }
}
