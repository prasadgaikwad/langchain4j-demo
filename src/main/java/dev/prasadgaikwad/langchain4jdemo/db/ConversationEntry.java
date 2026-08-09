package dev.prasadgaikwad.langchain4jdemo.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single chat message persisted to the H2 database. Enables conversation
 * history that survives a restart, unlike the in-memory {@code ChatMemory}.
 */
@Entity
@Table(name = "conversation_entries")
public class ConversationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String conversationId;

    private String role;

    @Column(length = 4000)
    private String text;

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
