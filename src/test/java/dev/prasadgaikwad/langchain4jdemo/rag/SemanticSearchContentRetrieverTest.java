package dev.prasadgaikwad.langchain4jdemo.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticSearchContentRetrieverTest {

    @TempDir
    Path tempDir;

    @Test
    void retrieveReturnsRankedContentFromStore() throws Exception {
        Path docs = tempDir.resolve("docs");
        Files.createDirectories(docs);
        Files.writeString(docs.resolve("java.txt"),
                "Java is a general-purpose programming language widely used in enterprise software.");
        Files.writeString(docs.resolve("cats.txt"),
                "Cats are small, furry animals often kept as pets.");

        SemanticSearchService searchService = newSemanticSearchService();
        searchService.indexDirectory(docs);
        SemanticSearchContentRetriever retriever = new SemanticSearchContentRetriever(searchService, 3);

        List<Content> contents = retriever.retrieve(Query.from("programming languages"));

        assertThat(contents).isNotEmpty();
        assertThat(contents.get(0).textSegment().text()).containsIgnoringCase("programming");
    }

    @Test
    void retrieveOnEmptyStoreReturnsNoContent() {
        SemanticSearchService searchService = newSemanticSearchService();
        SemanticSearchContentRetriever retriever = new SemanticSearchContentRetriever(searchService, 3);

        assertThat(retriever.retrieve(Query.from("anything"))).isEmpty();
    }

    private SemanticSearchService newSemanticSearchService() {
        return new SemanticSearchService(modelName -> new FakeEmbeddingModel(), "test", null, 3);
    }
}
