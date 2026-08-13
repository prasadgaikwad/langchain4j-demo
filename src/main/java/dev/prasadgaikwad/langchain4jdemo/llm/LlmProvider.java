package dev.prasadgaikwad.langchain4jdemo.llm;

import java.util.Locale;

/**
 * The LLM providers supported by the demo. Each provider maps to a LangChain4j
 * {@code ChatModel} implementation and a default model name; {@link LlmProvider#label()}
 * is used in the CLI to address a provider as {@code provider:model}.
 */
public enum LlmProvider {

    OPENAI("openai", "gpt-4o-mini"),
    ANTHROPIC("anthropic", "claude-haiku-4-5-20251001"),
    GEMINI("gemini", "gemini-2.5-flash"),
    OLLAMA("ollama", "llama3.2");

    private final String label;
    private final String defaultModelName;

    LlmProvider(String label, String defaultModelName) {
        this.label = label;
        this.defaultModelName = defaultModelName;
    }

    public String label() {
        return label;
    }

    public String defaultModelName() {
        return defaultModelName;
    }

    public static LlmProvider fromLabel(String label) {
        for (LlmProvider provider : values()) {
            if (provider.label.equals(label.trim().toLowerCase(Locale.ROOT))) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown provider: '" + label
                + "' (use: openai | anthropic | gemini | ollama)");
    }
}
