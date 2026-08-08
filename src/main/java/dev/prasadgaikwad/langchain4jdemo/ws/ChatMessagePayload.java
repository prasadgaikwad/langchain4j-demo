package dev.prasadgaikwad.langchain4jdemo.ws;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatMessagePayload(@JsonProperty("message") String message) {
}
