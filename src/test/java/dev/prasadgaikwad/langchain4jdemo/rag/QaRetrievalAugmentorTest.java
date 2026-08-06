package dev.prasadgaikwad.langchain4jdemo.rag;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QaRetrievalAugmentorTest {

    @TempDir
    Path tempDir;

    @Test
    void augmentRetrievesContextAndInjectsItIntoTheUserMessage() throws Exception {
        Path docs = tempDir.resolve("docs");
        Files.createDirectories(docs);
        Files.writeString(docs.resolve("rag.txt"),
                "Retrieval Augmented Generation combines a vector database with a chat model "
                        + "so the model can answer questions from your own documents.");

        SemanticSearchService searchService = new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
        searchService.indexDirectory(docs);

        SemanticSearchContentRetriever retriever = new SemanticSearchContentRetriever(searchService, 5);
        DefaultRetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();

        UserMessage question = UserMessage.from("How does RAG work?");
        AugmentationResult result = augmentor.augment(new AugmentationRequest(
                question, Metadata.from(question, "qa", List.of())));

        assertThat(result.contents()).isNotEmpty();
        assertThat(result.contents()).allSatisfy(content ->
                assertThat(content.textSegment().text()).containsIgnoringCase("Retrieval Augmented Generation"));
        assertThat(((UserMessage) result.chatMessage()).singleText()).contains("Retrieval Augmented Generation");
    }

    @Test
    void augmentWithEmptyStoreLeavesTheUserMessageUntouched() {
        SemanticSearchService searchService = new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
        SemanticSearchContentRetriever retriever = new SemanticSearchContentRetriever(searchService, 5);
        DefaultRetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();

        UserMessage question = UserMessage.from("Is there anything indexed?");
        AugmentationResult result = augmentor.augment(new AugmentationRequest(
                question, Metadata.from(question, "qa", List.of())));

        assertThat(result.contents()).isEmpty();
        assertThat(((UserMessage) result.chatMessage()).singleText()).isEqualTo("Is there anything indexed?");
    }
}
