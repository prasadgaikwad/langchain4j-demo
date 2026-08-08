package dev.prasadgaikwad.langchain4jdemo.prompt;

/**
 * The three-way sentiment label used by the few-shot classification demo.
 * Returning an enum from an AI Service tells LangChain4j to constrain the model
 * to one of these values and parse the reply into the enum constant.
 */
public enum Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}
