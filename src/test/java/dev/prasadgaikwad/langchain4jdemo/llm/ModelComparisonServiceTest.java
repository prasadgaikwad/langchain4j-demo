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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelComparisonServiceTest {

    private SemanticSearchService searchService() {
        return new SemanticSearchService(
                modelName -> new FakeEmbeddingModel(), new DocumentService("recursive", 200, 20), "test", null, 5);
    }

    @Test
    void evaluatesEveryAvailableModelWithIsolatedModelsWithoutMutatingRegistry() {
        FakeChatModel openai = new FakeChatModel("openai answer");
        FakeChatModel anthropic = new FakeChatModel("anthropic answer");
        Map<String, ChatModel> models = new LinkedHashMap<>();
        models.put("openai:gpt-4o-mini", openai);
        models.put("anthropic:claude-haiku-4-5-20251001", anthropic);

        ModelRegistry registry = new ModelRegistry(LlmProvider.OPENAI, "gpt-4o-mini", models);
        EvaluationService evaluationService = new EvaluationService(searchService(), new FakeChatModel("judge"));
        ModelComparisonService comparison =
                new ModelComparisonService(registry, evaluationService);

        // A provider factory that resolves the per-model ChatModel so it can
        // assert on which model was actually used. In production the factory would
        // be backed by ModelScopedServices.
        Map<String, ChatModel> usedDuringRun = new LinkedHashMap<>();
        java.util.function.Function<String, AnswerProvider> providerFactory = label -> {
            ChatModel chatModel = registry.chatModelFor(label);
            usedDuringRun.put(label, chatModel);
            return question -> "POSITIVE";
        };

        ComparisonReport report = comparison.compare(
                GoldenDataset.sentiment(),
                providerFactory);

        assertThat(report.dataset()).isEqualTo("sentiment");
        assertThat(report.models()).hasSize(2);

        // The registry selection must be untouched by the comparison.
        assertThat(registry.currentProvider()).isEqualTo(LlmProvider.OPENAI);
        assertThat(registry.currentLabel()).isEqualTo("openai:gpt-4o-mini");
        assertThat(registry.currentChatModel()).isSameAs(openai);

        // Each model used its own isolated instance; the shared selection is not
        // repointed during the run.
        assertThat(usedDuringRun).containsKeys(
                "openai:gpt-4o-mini", "anthropic:claude-haiku-4-5-20251001");
        assertThat(usedDuringRun.get("openai:gpt-4o-mini")).isSameAs(openai);
        assertThat(usedDuringRun.get("anthropic:claude-haiku-4-5-20251001")).isSameAs(anthropic);
    }

    @Test
    void comparisonIsIndependentOfCurrentSelectionRepointing() {
        FakeChatModel openai = new FakeChatModel("openai answer");
        FakeChatModel anthropic = new FakeChatModel("anthropic answer");
        Map<String, ChatModel> models = new LinkedHashMap<>();
        models.put("openai:gpt-4o-mini", openai);
        models.put("anthropic:claude-haiku-4-5-20251001", anthropic);
        ModelRegistry registry = new ModelRegistry(LlmProvider.OPENAI, "gpt-4o-mini", models);
        ModelComparisonService comparison =
                new ModelComparisonService(registry, new EvaluationService(searchService(), new FakeChatModel("judge")));

        // A concurrent /model chat switch during the comparison must not affect it.
        registry.setModel("anthropic");
        ComparisonReport report = comparison.compare(
                GoldenDataset.sentiment(),
                label -> question -> "POSITIVE");

        assertThat(registry.currentLabel()).isEqualTo("anthropic:claude-haiku-4-5-20251001");
        assertThat(report.models()).hasSize(2)
                .extracting(ModelScore::model)
                .contains("openai:gpt-4o-mini", "anthropic:claude-haiku-4-5-20251001");
    }
}
