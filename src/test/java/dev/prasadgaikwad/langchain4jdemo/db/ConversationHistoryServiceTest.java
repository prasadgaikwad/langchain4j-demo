package dev.prasadgaikwad.langchain4jdemo.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ConversationHistoryServiceTest {

    @Autowired
    ConversationEntryRepository repository;

    @Test
    void recordsAndReadsHistoryInOrder() {
        ConversationHistoryService service = new ConversationHistoryService(repository);
        service.record("conv-1", "user", "hello");
        service.record("conv-1", "ai", "hi there");

        List<ConversationEntry> history = service.history("conv-1");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getRole()).isEqualTo("user");
        assertThat(history.get(0).getText()).isEqualTo("hello");
        assertThat(history.get(1).getRole()).isEqualTo("ai");
        assertThat(history.get(1).getText()).isEqualTo("hi there");
    }

    @Test
    void returnsEmptyHistoryForUnknownConversation() {
        ConversationHistoryService service = new ConversationHistoryService(repository);
        assertThat(service.history("nobody")).isEmpty();
    }

    @Test
    void listsDistinctConversationIds() {
        ConversationHistoryService service = new ConversationHistoryService(repository);
        service.record("conv-a", "user", "1");
        service.record("conv-a", "ai", "2");
        service.record("conv-b", "user", "3");

        assertThat(service.conversationIds()).containsExactlyInAnyOrder("conv-a", "conv-b");
    }

    @Test
    void clearsOnlyTheRequestedConversation() {
        ConversationHistoryService service = new ConversationHistoryService(repository);
        service.record("conv-a", "user", "1");
        service.record("conv-b", "user", "2");

        service.clear("conv-a");

        assertThat(service.history("conv-a")).isEmpty();
        assertThat(service.history("conv-b")).hasSize(1);
    }
}
