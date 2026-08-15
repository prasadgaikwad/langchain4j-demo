package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateful custom tool: notes are stored per conversation via
 * {@link ToolMemoryId}, which injects the current conversation's memory id into
 * the tool call. The same note list is therefore scoped to one conversation and
 * never leaks into another.
 */
@Component
public class NoteTool {

    private final Map<String, List<String>> notesByConversation = new ConcurrentHashMap<>();

    @Tool("Saves a note for the current conversation")
    public String saveNote(@ToolMemoryId String memoryId,
                           @P("The note text to save") String note) {
        notesByConversation.computeIfAbsent(memoryId, key -> new java.util.ArrayList<>()).add(note);
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
