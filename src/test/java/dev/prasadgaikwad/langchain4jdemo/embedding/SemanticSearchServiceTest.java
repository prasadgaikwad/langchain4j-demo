package dev.prasadgaikwad.langchain4jdemo.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticSearchServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void searchOnEmptyStoreReturnsNoResults() {
        SemanticSearchService service = new SemanticSearchService(new FakeEmbeddingModel(), null, 3);

        assertThat(service.search("anything")).isEmpty();
    }

    @Test
    void indexDirectoryAndSearchReturnsMostRelevantSegment() throws Exception {
        Path dataDir = tempDir.resolve("docs");
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve("java.txt"),
                "Java is a general-purpose programming language widely used in enterprise software development.");
        Files.writeString(dataDir.resolve("cats.txt"),
                "Cats are small, furry animals often kept as pets and known for their independence.");

        SemanticSearchService service = new SemanticSearchService(new FakeEmbeddingModel(), null, 3);
        int indexed = service.indexDirectory(dataDir);

        assertThat(indexed).isGreaterThan(0);
        assertThat(service.storeSize()).isEqualTo(indexed);

        List<EmbeddingMatch<TextSegment>> matches = service.search("programming languages");

        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).embedded().text()).containsIgnoringCase("programming");
    }

    @Test
    void indexSingleDocumentAndSaveLoadRoundTrip() throws Exception {
        Path document = tempDir.resolve("doc.txt");
        Files.writeString(document, "Retrieval augmented generation combines a vector database with a chat model.");
        Path storeFile = tempDir.resolve("store.json");

        SemanticSearchService first = new SemanticSearchService(new FakeEmbeddingModel(), storeFile.toString(), 3);
        assertThat(first.indexDocument(document)).isGreaterThan(0);
        assertThat(first.storeSize()).isGreaterThan(0);

        SemanticSearchService second = new SemanticSearchService(new FakeEmbeddingModel(), storeFile.toString(), 3);
        assertThat(second.storeSize()).isEqualTo(first.storeSize());

        List<EmbeddingMatch<TextSegment>> matches = second.search("vector database");
        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).embedded().text()).containsIgnoringCase("vector database");
    }

    @Test
    void embedReturnsVectorOfModelDimension() {
        SemanticSearchService service = new SemanticSearchService(new FakeEmbeddingModel(), null, 3);

        Embedding embedding = service.embed("hello world");

        assertThat(embedding.dimension()).isEqualTo(FakeEmbeddingModel.DIM);
    }

    @Test
    void switchingModelUpdatesModelName() {
        SemanticSearchService service = new SemanticSearchService(new FakeEmbeddingModel(), null, 3);

        service.setEmbeddingModel("test-model");

        assertThat(service.modelName()).isEqualTo("test-model");
    }

    /**
     * Deterministic, locally-computed embedding model used to avoid real API calls in tests.
     * Each text is mapped to a vector of character-bigram counts, so texts sharing substrings
     * score as more similar.
     */
    static class FakeEmbeddingModel implements EmbeddingModel {

        static final int DIM = 64;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            return Response.from(textSegments.stream()
                    .map(segment -> Embedding.from(vector(segment.text())))
                    .toList());
        }

        @Override
        public Response<Embedding> embed(TextSegment textSegment) {
            return Response.from(Embedding.from(vector(textSegment.text())));
        }

        private static float[] vector(String text) {
            float[] vector = new float[DIM];
            String normalized = text.toLowerCase();
            for (int i = 0; i < normalized.length() - 1; i++) {
                int hash = (normalized.charAt(i) * 31 + normalized.charAt(i + 1)) & 0x7fffffff;
                vector[hash % DIM] += 1.0f;
            }
            return vector;
        }
    }
}
