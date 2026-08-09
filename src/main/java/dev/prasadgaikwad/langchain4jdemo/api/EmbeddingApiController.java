package dev.prasadgaikwad.langchain4jdemo.api;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for indexing documents and searching the embedding store.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Embeddings", description = "Index documents and search the embedding store")
public class EmbeddingApiController {

    private final SemanticSearchService searchService;

    public EmbeddingApiController(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/index")
    @Operation(summary = "Index a file or directory into the embedding store",
            description = "Loads and splits a document (or every document in a directory), "
                    + "embeds each chunk, and adds it to the store.")
    @ApiResponse(responseCode = "200", description = "The number of chunks indexed and the new store size",
            content = @Content(schema = @Schema(type = "object")))
    @ApiResponse(responseCode = "404", description = "Path not found")
    public Map<String, Object> index(@Parameter(description = "Path to a file or directory (txt, md, pdf)",
            example = "sample-data")
                                     @RequestParam String path) {
        Path pathValue = Path.of(path);
        int indexed;
        if (Files.isDirectory(pathValue)) {
            indexed = searchService.indexDirectory(pathValue);
        } else if (Files.isRegularFile(pathValue)) {
            indexed = searchService.indexDocument(pathValue);
        } else {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Path not found: " + path);
            return error;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indexed", indexed);
        result.put("storeSize", searchService.storeSize());
        return result;
    }

    @GetMapping("/search")
    @Operation(summary = "Semantic search over the indexed documents",
            description = "Embeds the query and returns the most similar chunks with relevance scores.")
    @ApiResponse(responseCode = "200", description = "Search hits ranked by relevance",
            content = @Content(schema = @Schema(implementation = SearchResult.class)))
    public List<SearchResult> search(@Parameter(description = "Search query", example = "What is RAG?")
                                     @RequestParam String q) {
        return searchService.search(q).stream()
                .map(match -> new SearchResult(match.score(), match.embedded().text()))
                .toList();
    }

    @GetMapping("/store")
    @Operation(summary = "Embedding store stats",
            description = "Current model name, store size, and the JSON file the store persists to.")
    @ApiResponse(responseCode = "200", description = "Store statistics",
            content = @Content(schema = @Schema(type = "object")))
    public Map<String, Object> store() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", searchService.modelName());
        result.put("storeSize", searchService.storeSize());
        Path storePath = searchService.storePath();
        result.put("storeFile", storePath != null ? storePath.toString() : null);
        return result;
    }
}
