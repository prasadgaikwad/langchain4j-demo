package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool that searches the indexed documents, giving the agent access to the
 * embedded knowledge base while it is executing a task.
 */
@Component
public class DocumentSearchTool {

    private final SemanticSearchService searchService;

    public DocumentSearchTool(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @Tool("Searches the indexed documents and returns the most relevant passages with their relevance scores")
    public String searchDocuments(@P("The search query, e.g. \"what does the document say about RAG\"") String query) {
        List<EmbeddingMatch<TextSegment>> matches = searchService.search(query);
        if (matches.isEmpty()) {
            return "No matching documents found. Documents may need to be indexed first with /index.";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            result.append(i + 1).append(". [score ")
                    .append(String.format("%.4f", match.score()))
                    .append("] ")
                    .append(match.embedded().text())
                    .append("\n");
        }
        return result.toString().trim();
    }
}
