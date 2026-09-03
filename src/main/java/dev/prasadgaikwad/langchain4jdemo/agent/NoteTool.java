package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateful custom tool: notes are stored per conversation via
 * {@link ToolMemoryId}, which injects the current conversation's memory id into
 * the tool call. The same note list is therefore scoped to one conversation and
 * never leaks into another.
 * <p>
 * The {@code notesByConversation} map is an access-order LRU capped at
 * {@code app.memory.note-tool-max-entries}, so a long-running process that sees
 * many conversation ids does not leak memory (issue #266).
 */
@Component
public class NoteTool {

    private final Map<String, List<String>> notesByConversation;

    public NoteTool(@Value("${app.memory.note-tool-max-entries:100}") int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.notesByConversation =
                Collections.synchronizedMap(new LinkedHashMap<>(maxEntries, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
                        return size() > maxEntries;
                    }
                });
    }

    @Tool("Saves a note for the current conversation")
    public String saveNote(@ToolMemoryId String memoryId,
                           @P("The note text to save") String note) {
        notesByConversation.computeIfAbsent(memoryId, key -> new ArrayList<>()).add(note);
        return "Note saved for conversation " + memoryId + ".";
    }

    @Tool("Lists the notes saved in the current conversation")
    public String listNotes(@ToolMemoryId String memoryId) {
        List<String> notes = notesByConversation.getOrDefault(memoryId, List.of());
        if (notes.isEmpty()) {
            return "No notes saved in this conversation yet.";
        }
        StringBuilder result = new StringBuilder("Notes for conversation " + memoryId + ":");
        for (int i = 0; i < notes.size(); i++) {
            result.append("\n").append(i + 1).append(". ").append(notes.get(i));
        }
        return result.toString();
    }
}
