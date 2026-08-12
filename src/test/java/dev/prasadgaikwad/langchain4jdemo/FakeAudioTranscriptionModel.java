package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.model.audio.AudioTranscriptionModel;
import dev.langchain4j.model.audio.AudioTranscriptionRequest;
import dev.langchain4j.model.audio.AudioTranscriptionResponse;

/**
 * Deterministic transcription model used to avoid real API calls in tests.
 * Captures the transcription request so tests can assert on the audio that was
 * sent.
 */
public class FakeAudioTranscriptionModel implements AudioTranscriptionModel {

    private AudioTranscriptionRequest lastRequest;

    @Override
    public AudioTranscriptionResponse transcribe(AudioTranscriptionRequest request) {
        this.lastRequest = request;
        return new AudioTranscriptionResponse("Hello from the recording.");
    }

    public AudioTranscriptionRequest lastRequest() {
        return lastRequest;
    }
}
