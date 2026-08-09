package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.db.ConversationEntry;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST endpoints over the database-backed conversation history.
 */
@RestController
@RequestMapping("/api/history")
public class HistoryApiController {

    private final ConversationHistoryService historyService;

    public HistoryApiController(ConversationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<String> conversations() {
        return historyService.conversationIds();
    }

    @GetMapping("/{conversationId}")
    public List<ConversationEntry> history(@PathVariable String conversationId) {
        List<ConversationEntry> entries = historyService.history(conversationId);
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No history for conversation '" + conversationId + "'");
        }
        return entries;
    }

    @DeleteMapping("/{conversationId}")
    public void clear(@PathVariable String conversationId) {
        historyService.clear(conversationId);
    }
}
