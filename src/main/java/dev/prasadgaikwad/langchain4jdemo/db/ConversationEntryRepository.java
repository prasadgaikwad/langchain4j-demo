package dev.prasadgaikwad.langchain4jdemo.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConversationEntryRepository extends JpaRepository<ConversationEntry, Long> {

    List<ConversationEntry> findByConversationIdOrderByTimestampAsc(String conversationId);

    void deleteByConversationId(String conversationId);

    @Query("select distinct e.conversationId from ConversationEntry e order by e.conversationId")
    List<String> findDistinctConversationIds();
}
