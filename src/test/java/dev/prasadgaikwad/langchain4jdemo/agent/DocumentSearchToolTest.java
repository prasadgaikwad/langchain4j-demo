package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSearchToolTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsRankedMatchesFromTheIndexedDocuments() throws Exception {
        Path docs = tempDir.resolve("docs");
        Files.createDirectories(docs);
        Files.writeString(docs.resolve("langchain4j.txt"),
                "LangChain4j simplifies AI-powered applications. Agents use tools to complete tasks.");
        Files.writeString(docs.resolve("memory.txt"),
                "Conversation memory lets the chat model remember previous messages.");

        SemanticSearchService searchService = new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
        searchService.indexDirectory(docs);

        DocumentSearchTool tool = new DocumentSearchTool(searchService);
        String result = tool.searchDocuments("What are agents?");

        assertThat(result).contains("Agents use tools to complete tasks");
        assertThat(result).contains("score");
    }

    @Test
    void reportsWhenTheStoreIsEmpty() {
        SemanticSearchService searchService = new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
        DocumentSearchTool tool = new DocumentSearchTool(searchService);

        assertThat(tool.searchDocuments("anything")).contains("No matching documents found");
    }
}
