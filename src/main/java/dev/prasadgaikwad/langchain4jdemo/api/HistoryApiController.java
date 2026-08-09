package dev.prasadgaikwad.langchain4jdemo.api;

import dev.prasadgaikwad.langchain4jdemo.db.ConversationEntry;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "History", description = "Database-backed conversation history")
public class HistoryApiController {

    private final ConversationHistoryService historyService;

    public HistoryApiController(ConversationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "List all conversations",
            description = "Returns the distinct conversation ids that have persisted history.")
    @ApiResponse(responseCode = "200", description = "Conversation ids",
            content = @Content(schema = @Schema(type = "array", implementation = String.class)))
    public List<String> conversations() {
        return historyService.conversationIds();
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Fetch a conversation's message history",
            description = "Returns the persisted messages for one conversation in timestamp order.")
    @ApiResponse(responseCode = "200", description = "The conversation messages",
            content = @Content(schema = @Schema(implementation = ConversationEntry.class)))
    @ApiResponse(responseCode = "404", description = "No history for the conversation")
    public List<ConversationEntry> history(@Parameter(description = "Conversation id", example = "web")
                                           @PathVariable String conversationId) {
        List<ConversationEntry> entries = historyService.history(conversationId);
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No history for conversation '" + conversationId + "'");
        }
        return entries;
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Delete a conversation's history",
            description = "Removes all persisted messages for the conversation.")
    @ApiResponse(responseCode = "200", description = "History deleted")
    public void clear(@Parameter(description = "Conversation id", example = "web")
                      @PathVariable String conversationId) {
        historyService.clear(conversationId);
    }
}
