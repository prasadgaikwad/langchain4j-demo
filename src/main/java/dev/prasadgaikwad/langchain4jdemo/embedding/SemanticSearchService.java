package dev.prasadgaikwad.langchain4jdemo.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * Semantic search over a set of documents.
 * <p>
 * Documents are loaded, parsed, and split into {@link TextSegment}s by
 * {@link DocumentService}, then embedded with the configured
 * {@link EmbeddingModel} and stored in an {@link InMemoryEmbeddingStore} which
 * can be persisted to (and restored from) a JSON file.
 * <p>
 * The embedding model can be switched at runtime via {@link #setEmbeddingModel(String)}.
 */
@Service
public class SemanticSearchService {

    private final Function<String, EmbeddingModel> modelFactory;
    private final DocumentService documentService;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final Path storePath;
    private final int defaultMaxResults;
    private EmbeddingModel embeddingModel;
    private String modelName;

    @Autowired
    public SemanticSearchService(Function<String, EmbeddingModel> modelFactory,
                                 DocumentService documentService,
                                 @Value("${app.embedding.model-name:text-embedding-3-small}") String modelName,
                                 @Value("${app.embedding.store-path:}") String storePath,
                                 @Value("${app.embedding.max-results:5}") int defaultMaxResults) {
        this.modelFactory = modelFactory;
        this.documentService = documentService;
        this.modelName = modelName;
        this.embeddingModel = modelFactory.apply(modelName);
        this.storePath = storePath == null || storePath.isBlank() ? null : Path.of(storePath);
        this.defaultMaxResults = defaultMaxResults;
        this.embeddingStore = loadStore();
    }

    // package-private constructor for unit tests (no Spring, no API key needed)
    SemanticSearchService(EmbeddingModel embeddingModel, DocumentService documentService,
                          String storePath, int defaultMaxResults) {
        this(modelName -> embeddingModel, documentService, "test", storePath, defaultMaxResults);
    }

    private InMemoryEmbeddingStore<TextSegment> loadStore() {
        if (storePath != null && Files.exists(storePath)) {
            return InMemoryEmbeddingStore.fromFile(storePath);
        }
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Loads, parses, splits, embeds, and stores a single document file.
     *
     * @return the number of segments indexed
     */
    public int indexDocument(Path filePath) {
        return indexSegments(documentService.loadAndSplit(filePath));
    }

    /**
     * Loads, parses, splits, embeds, and stores all documents in a directory.
     *
     * @return the number of segments indexed
     */
    public int indexDirectory(Path directoryPath) {
        return indexSegments(documentService.loadAndSplitDirectory(directoryPath));
    }

    private int indexSegments(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            return 0;
        }
        int sizeBefore = embeddingStore.size();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        save();
        return embeddingStore.size() - sizeBefore;
    }

    /**
     * Embeds the query and returns the most similar stored segments, ranked by relevance score.
     */
    public List<EmbeddingMatch<TextSegment>> search(String query) {
        return search(query, defaultMaxResults);
    }

    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        Response<Embedding> response = embeddingModel.embed(query);
        return embeddingStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(response.content())
                        .maxResults(maxResults)
                        .build())
                .matches();
    }

    /**
     * Embeds a single text and returns its vector representation.
     */
    public Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }

    public void setEmbeddingModel(String modelName) {
        this.embeddingModel = modelFactory.apply(modelName);
        this.modelName = modelName;
    }

    public String modelName() {
        return modelName;
    }

    public int storeSize() {
        return embeddingStore.size();
    }

    public Path storePath() {
        return storePath;
    }

    /**
     * Persists the embedding store. Returns {@code false} if no path is available.
     */
    public boolean save() {
        return save(storePath);
    }

    public boolean save(Path path) {
        if (path == null) {
            return false;
        }
        embeddingStore.serializeToFile(path);
        return true;
    }
}
