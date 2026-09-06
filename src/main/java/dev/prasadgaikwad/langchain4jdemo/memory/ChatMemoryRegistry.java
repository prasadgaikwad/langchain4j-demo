package dev.prasadgaikwad.langchain4jdemo.memory;

import dev.langchain4j.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks the {@link ChatMemory} instances created by the {@code ChatMemoryProvider}.
 * {@code AiServices} retains memories itself, so this registry exposes them to the
 * CLI for inspection and clearing.
 * <p>
 * The map is an access-order LRU capped at {@code app.memory.registry-max-entries},
 * so a long-running process that sees many conversation ids does not leak memory
 * (issue #266): registering/reading a memory marks it recently used and the least
 * recently used entry is evicted once the cap is exceeded.
 */
@Component
public class ChatMemoryRegistry {

    private final Map<String, ChatMemory> memories;

    public ChatMemoryRegistry(@Value("${app.memory.registry-max-entries:100}") int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.memories = Collections.synchronizedMap(new LinkedHashMap<>(maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ChatMemory> eldest) {
                return size() > maxEntries;
            }
        });
    }

    public void register(String memoryId, ChatMemory memory) {
        memories.put(memoryId, memory);
    }

    public ChatMemory get(String memoryId) {
        return memories.get(memoryId);
    }
}
