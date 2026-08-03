package dev.prasadgaikwad.langchain4jdemo.embedding;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
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
 * Documents are split into {@link TextSegment}s, embedded with the configured
 * {@link EmbeddingModel}, and stored in an {@link InMemoryEmbeddingStore} which
 * can be persisted to (and restored from) a JSON file.
 * <p>
 * The embedding model can be switched at runtime via {@link #setEmbeddingModel(String)}.
 */
@Service
public class SemanticSearchService {

    private static final int MAX_SEGMENT_SIZE_IN_CHARS = 200;
    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 20;

    private final Function<String, EmbeddingModel> modelFactory;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final Path storePath;
    private final int defaultMaxResults;
    private EmbeddingModel embeddingModel;
    private String modelName;

    @Autowired
    public SemanticSearchService(Function<String, EmbeddingModel> modelFactory,
                                 @Value("${app.embedding.model-name:text-embedding-3-small}") String modelName,
                                 @Value("${app.embedding.store-path:}") String storePath,
                                 @Value("${app.embedding.max-results:5}") int defaultMaxResults) {
        this.modelFactory = modelFactory;
        this.modelName = modelName;
        this.embeddingModel = modelFactory.apply(modelName);
        this.storePath = storePath == null || storePath.isBlank() ? null : Path.of(storePath);
        this.defaultMaxResults = defaultMaxResults;
        this.embeddingStore = loadStore();
    }

    // package-private constructor for unit tests (no Spring, no API key needed)
    SemanticSearchService(EmbeddingModel embeddingModel, String storePath, int defaultMaxResults) {
        this(modelName -> embeddingModel, "test", storePath, defaultMaxResults);
    }

    private InMemoryEmbeddingStore<TextSegment> loadStore() {
        if (storePath != null && Files.exists(storePath)) {
            return InMemoryEmbeddingStore.fromFile(storePath);
        }
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Loads a single document file, splits it, embeds it, and stores the segments.
     *
     * @return the number of segments indexed
     */
    public int indexDocument(Path filePath) {
        return ingest(List.of(FileSystemDocumentLoader.loadDocument(filePath)));
    }

    /**
     * Loads all documents from a directory, splits them, embeds them, and stores the segments.
     *
     * @return the number of segments indexed
     */
    public int indexDirectory(Path directoryPath) {
        return ingest(FileSystemDocumentLoader.loadDocuments(directoryPath));
    }

    private int ingest(List<Document> documents) {
        int sizeBefore = embeddingStore.size();
        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(MAX_SEGMENT_SIZE_IN_CHARS, MAX_OVERLAP_SIZE_IN_CHARS))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(documents);
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
