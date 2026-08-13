package dev.prasadgaikwad.langchain4jdemo.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.evaluation.AnswerProvider;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationService;
import dev.prasadgaikwad.langchain4jdemo.evaluation.GoldenDataset;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelComparisonServiceTest {

    private SemanticSearchService searchService() {
        return new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
    }

    @Test
    void evaluatesEveryAvailableModelAndRestoresTheOriginalSelection() {
        FakeChatModel openai = new FakeChatModel("openai answer");
        FakeChatModel anthropic = new FakeChatModel("anthropic answer");
        Map<String, ChatModel> models = new LinkedHashMap<>();
        models.put("openai:gpt-4o-mini", openai);
        models.put("anthropic:claude-haiku-4-5-20251001", anthropic);
        ModelRegistry registry = new ModelRegistry(LlmProvider.OPENAI, "gpt-4o-mini", models);
        ModelComparisonService comparison =
                new ModelComparisonService(registry, new EvaluationService(searchService(), new FakeChatModel("1")));

        List<String> usedDuringRun = new ArrayList<>();
        AnswerProvider provider = question -> {
            usedDuringRun.add(registry.currentLabel());
            return "POSITIVE";
        };

        ComparisonReport report = comparison.compare(GoldenDataset.sentiment(), provider);

        assertThat(report.dataset()).isEqualTo("sentiment");
        assertThat(report.models()).hasSize(2);
        assertThat(report.models().get(0).model()).isEqualTo("anthropic:claude-haiku-4-5-20251001");
        assertThat(report.models().get(1).model()).isEqualTo("openai:gpt-4o-mini");
        assertThat(report.models()).allSatisfy(row ->
                assertThat(row.scores()).containsKeys("exact", "contains", "f1", "rougeL", "embed", "judge"));
        assertThat(report.models().stream().flatMap(row -> row.scores().values().stream()))
                .allSatisfy(score -> assertThat(score).isBetween(0.0, 1.0));
        assertThat(usedDuringRun).hasSize(6)
                .contains("anthropic:claude-haiku-4-5-20251001", "openai:gpt-4o-mini");

        assertThat(registry.currentProvider()).isEqualTo(LlmProvider.OPENAI);
        assertThat(registry.currentLabel()).isEqualTo("openai:gpt-4o-mini");
        assertThat(registry.currentChatModel()).isSameAs(openai);
        assertThat(anthropic.lastRequest()).isNull();
    }
}
