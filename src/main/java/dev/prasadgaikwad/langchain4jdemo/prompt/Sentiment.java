package dev.prasadgaikwad.langchain4jdemo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The three-way sentiment label used by the few-shot classification demo.
 * Returning an enum from an AI Service tells LangChain4j to constrain the model
 * to one of these values and parse the reply into the enum constant.
 */
@Schema(description = "The classified sentiment", enumAsRef = true)
public enum Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}
