package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.Tool;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Tool that exposes the current state of the embedding store to the agent.
 */
@Component
public class EmbeddingStoreStatsTool {

    private final SemanticSearchService searchService;

    public EmbeddingStoreStatsTool(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @Tool("Returns statistics about the embedding store, such as the number of indexed segments and the active embedding model")
    public String getEmbeddingStoreStats() {
        Path storePath = searchService.storePath();
        return "Model: " + searchService.modelName()
                + ", segments indexed: " + searchService.storeSize()
                + ", store file: " + (storePath != null ? storePath : "(none)");
    }
}
