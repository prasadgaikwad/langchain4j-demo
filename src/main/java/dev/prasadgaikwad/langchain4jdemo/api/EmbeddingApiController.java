package dev.prasadgaikwad.langchain4jdemo.api;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
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
public class EmbeddingApiController {

    private final SemanticSearchService searchService;

    public EmbeddingApiController(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/index")
    public Map<String, Object> index(@RequestParam String path) {
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
    public List<SearchResult> search(@RequestParam String q) {
        return searchService.search(q).stream()
                .map(match -> new SearchResult(match.score(), match.embedded().text()))
                .toList();
    }

    @GetMapping("/store")
    public Map<String, Object> store() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", searchService.modelName());
        result.put("storeSize", searchService.storeSize());
        Path storePath = searchService.storePath();
        result.put("storeFile", storePath != null ? storePath.toString() : null);
        return result;
    }
}
