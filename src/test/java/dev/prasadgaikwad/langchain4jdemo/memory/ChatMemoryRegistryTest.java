package dev.prasadgaikwad.langchain4jdemo.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMemoryRegistryTest {

    @Test
    void returnsTheRegisteredMemory() {
        ChatMemoryRegistry registry = new ChatMemoryRegistry(100);
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        registry.register("memory-1", memory);

        assertThat(registry.get("memory-1")).isSameAs(memory);
    }

    @Test
    void leastRecentlyUsedMemoryIsEvictedPastTheCap() {
        ChatMemoryRegistry registry = new ChatMemoryRegistry(2);
        ChatMemory memory1 = MessageWindowChatMemory.withMaxMessages(10);
        ChatMemory memory2 = MessageWindowChatMemory.withMaxMessages(10);
        ChatMemory memory3 = MessageWindowChatMemory.withMaxMessages(10);

        registry.register("m1", memory1);
        registry.register("m2", memory2);
        registry.get("m1");
        registry.register("m3", memory3);

        assertThat(registry.get("m1")).isSameAs(memory1);
        assertThat(registry.get("m2")).isNull();
        assertThat(registry.get("m3")).isSameAs(memory3);
    }

    @Test
    void rejectsANonPositiveCap() {
        assertThatThrownBy(() -> new ChatMemoryRegistry(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
