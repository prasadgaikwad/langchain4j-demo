package dev.prasadgaikwad.langchain4jdemo.evaluation;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import dev.prasadgaikwad.langchain4jdemo.ai.QaAssistant;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.rag.SemanticSearchContentRetriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationServiceTest {

    @TempDir
    Path tempDir;

    private SemanticSearchService searchService() {
        return new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
    }

    @Test
    void evaluatesEveryQuestionAndAggregatesAverages() {
        EvaluationService service = new EvaluationService(searchService(), new FakeChatModel("4"));
        AnswerProvider provider = question -> question.contains("loved") ? "POSITIVE" : "NEUTRAL";

        EvaluationReport report = service.evaluate(GoldenDataset.sentiment(), provider);

        assertThat(report.dataset()).isEqualTo("sentiment");
        assertThat(report.items()).hasSize(3);
        assertThat(report.averageScores()).containsKeys("exact", "contains", "f1", "rougeL", "embed", "judge");
        assertThat(report.averageScores().values()).allSatisfy(score -> assertThat(score).isBetween(0.0, 1.0));
        assertThat(report.items().get(0).scores().get("exact")).isEqualTo(1.0);
    }

    @Test
    void judgeMetricRunsAgainstTheConfiguredChatModel() {
        FakeChatModel judge = new FakeChatModel("5");
        EvaluationService service = new EvaluationService(searchService(), judge);
        AnswerProvider provider = question -> "POSITIVE";

        EvaluationReport report = service.evaluate(GoldenDataset.sentiment(), provider);

        assertThat(report.items().get(0).scores().get("judge")).isEqualTo(1.0);
        assertThat(judge.lastUserMessage()).contains("Produced answer");
    }

    /**
     * A full LLM-response test suite: the real {@code QaAssistant} AI-service
     * proxy (retrieval augmentor + chat model) is built on fake models, a
     * document is indexed, and the RAG golden dataset is scored end to end.
     * Because the fakes are deterministic, the suite runs offline and produces
     * a stable report that can be enforced in CI.
     */
    @Test
    void ragSuiteScoresTheGoldenDatasetThroughTheRealAiServiceProxy() throws Exception {
        Path doc = tempDir.resolve("docs");
        Files.createDirectories(doc);
        Files.writeString(doc.resolve("rag.txt"),
                "LangChain4j offers MessageWindowChatMemory and TokenWindowChatMemory for conversation memory. "
                        + "Semantic search embeds the query and finds stored vectors with the highest cosine similarity. "
                        + "LangChain4j is a Java framework that simplifies building applications with LLMs.");

        SemanticSearchService searchService = searchService();
        searchService.indexDirectory(doc);

        FakeChatModel chatModel = new FakeChatModel(
                "LangChain4j offers MessageWindowChatMemory and TokenWindowChatMemory.");
        ContentRetriever retriever = new SemanticSearchContentRetriever(searchService, 5);
        QaAssistant qaAssistant = AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(DefaultRetrievalAugmentor.builder().contentRetriever(retriever).build())
                .build();
        QaService qaService = new QaService(qaAssistant);
        EvaluationService evaluationService = new EvaluationService(searchService, chatModel);

        EvaluationReport report = evaluationService.evaluate(GoldenDataset.rag(), question -> qaService.ask("rag", question));

        assertThat(report.dataset()).isEqualTo("rag");
        assertThat(report.sampleCount()).isEqualTo(3);
        assertThat(report.averageScores()).containsKeys("exact", "contains", "f1", "rougeL", "embed", "judge");
        assertThat(report.items()).allSatisfy(item ->
                assertThat(item.actual()).isEqualTo(
                        "LangChain4j offers MessageWindowChatMemory and TokenWindowChatMemory."));
    }
}
