package dev.prasadgaikwad.langchain4jdemo.memory;

/**
 * Supported chat memory types, mirroring the README's "Buffer" and "Summary"
 * concepts as implemented by LangChain4j 1.19.0:
 * <ul>
 *   <li>{@link #MESSAGE_WINDOW} - sliding window limited by message count (a buffer)</li>
 *   <li>{@link #TOKEN_WINDOW}  - sliding window limited by token count (context-window-aware)</li>
 * </ul>
 */
public enum MemoryType {

    MESSAGE_WINDOW("message-window"),
    TOKEN_WINDOW("token-window");

    private final String label;

    MemoryType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Returns the {@link dev.langchain4j.memory.ChatMemory} id used for the given conversation.
     * The type is embedded in the id so that switching memory types starts a fresh memory
     * of the new type (ids are managed per type by {@code AiServices}).
     */
    public String memoryId(String conversationId) {
        return label + ":" + conversationId;
    }

    public static MemoryType fromLabel(String label) {
        for (MemoryType type : values()) {
            if (type.label.equalsIgnoreCase(label) || type.name().equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown memory type: " + label);
    }
}
