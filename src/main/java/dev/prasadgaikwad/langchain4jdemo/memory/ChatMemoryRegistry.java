package dev.prasadgaikwad.langchain4jdemo.memory;

import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks the {@link ChatMemory} instances created by the {@code ChatMemoryProvider}.
 * {@code AiServices} retains memories itself, so this registry exposes them to the
 * CLI for inspection and clearing.
 */
@Component
public class ChatMemoryRegistry {

    private final ConcurrentMap<String, ChatMemory> memories = new ConcurrentHashMap<>();

    public void register(String memoryId, ChatMemory memory) {
        memories.put(memoryId, memory);
    }

    public ChatMemory get(String memoryId) {
        return memories.get(memoryId);
    }
}
