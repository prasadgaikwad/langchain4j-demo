package dev.prasadgaikwad.langchain4jdemo.db;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists and reads chat messages in the H2 database. This is the
 * database-integration stage of the demo: conversation history lives outside the
 * in-memory {@code ChatMemoryRegistry} and survives restarts.
 */
@Service
@Transactional
public class ConversationHistoryService {

    private final ConversationEntryRepository repository;

    public ConversationHistoryService(ConversationEntryRepository repository) {
        this.repository = repository;
    }

    public ConversationEntry record(String conversationId, String role, String text) {
        return repository.save(new ConversationEntry(conversationId, role, text));
    }

    public List<ConversationEntry> history(String conversationId) {
        return repository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    public List<String> conversationIds() {
        return repository.findDistinctConversationIds();
    }

    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }
}
