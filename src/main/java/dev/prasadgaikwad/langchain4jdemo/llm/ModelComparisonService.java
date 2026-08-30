package dev.prasadgaikwad.langchain4jdemo.llm;

import dev.prasadgaikwad.langchain4jdemo.evaluation.AnswerProvider;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationReport;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationService;
import dev.prasadgaikwad.langchain4jdemo.evaluation.GoldenDataset;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Runs a golden dataset against every available model (see
 * {@link ModelRegistry#availableModels()}) and collects the averaged scores into
 * a {@link ComparisonReport}.
 * <p>
 * Each candidate model is evaluated with an <b>isolated</b> instance built via
 * {@link ModelRegistry#chatModelFor(String)} plus a model-scoped
 * {@link AnswerProvider} and judge produced by the {@code providerFactory}.
 * The shared {@link ModelRegistry} selection is never mutated, so live traffic
 * is unaffected by a running comparison (issue #267).
 */
@Service
public class ModelComparisonService {

    private final ModelRegistry modelRegistry;
    private final EvaluationService evaluationService;

    public ModelComparisonService(ModelRegistry modelRegistry, EvaluationService evaluationService) {
        this.modelRegistry = modelRegistry;
        this.evaluationService = evaluationService;
    }

    /**
     * @param providerFactory maps a {@code provider:model} label to an
     *                        {@link AnswerProvider} built against an isolated
     *                        model for that label
     */
    public ComparisonReport compare(GoldenDataset dataset, Function<String, AnswerProvider> providerFactory) {
        List<ModelScore> rows = new ArrayList<>();
        for (String model : modelRegistry.availableModels()) {
            var chatModel = modelRegistry.chatModelFor(model);
            EvaluationReport report =
                    evaluationService.evaluate(dataset, providerFactory.apply(model), chatModel);
            rows.add(new ModelScore(model, report.averageScores()));
        }
        return new ComparisonReport(dataset.name(), rows);
    }
}
