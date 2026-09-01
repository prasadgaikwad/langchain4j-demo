package dev.prasadgaikwad.langchain4jdemo.db;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single chat message persisted to the H2 database. Enables conversation
 * history that survives a restart, unlike the in-memory {@code ChatMemory}.
 */
@Entity
@Table(name = "conversation_entries")
@Schema(description = "One persisted chat message in a conversation")
public class ConversationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated id")
    private Long id;

    @Schema(description = "Conversation (memory) id the message belongs to", example = "web")
    private String conversationId;

    @Schema(description = "Message role", example = "user")
    private String role;

    @Lob
    @Schema(description = "The message text", example = "Hello! How do I use RAG?")
    private String text;

    @Schema(description = "When the message was recorded (ISO-8601)")
    private Instant timestamp;

    protected ConversationEntry() {
    }

    public ConversationEntry(String conversationId, String role, String text) {
        this.conversationId = conversationId;
        this.role = role;
        this.text = text;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
