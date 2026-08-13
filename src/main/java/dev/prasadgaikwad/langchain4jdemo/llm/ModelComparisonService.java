package dev.prasadgaikwad.langchain4jdemo.llm;

import dev.prasadgaikwad.langchain4jdemo.evaluation.AnswerProvider;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationReport;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationService;
import dev.prasadgaikwad.langchain4jdemo.evaluation.GoldenDataset;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a golden dataset against every available model (see
 * {@link ModelRegistry#availableModels()}) and collects the averaged scores into
 * a {@link ComparisonReport}. Because the {@link ModelRegistry} is the single
 * chat-model bean, switching the selection switches the entire pipeline (and
 * the LLM-as-a-judge metric) for each model in the comparison.
 * <p>
 * The user's current model selection is restored after the comparison.
 */
@Service
public class ModelComparisonService {

    private final ModelRegistry modelRegistry;
    private final EvaluationService evaluationService;

    public ModelComparisonService(ModelRegistry modelRegistry, EvaluationService evaluationService) {
        this.modelRegistry = modelRegistry;
        this.evaluationService = evaluationService;
    }

    public ComparisonReport compare(GoldenDataset dataset, AnswerProvider provider) {
        LlmProvider originalProvider = modelRegistry.currentProvider();
        String originalModelName = modelRegistry.currentModelName();
        try {
            List<ModelScore> rows = new ArrayList<>();
            for (String model : modelRegistry.availableModels()) {
                modelRegistry.setModel(model);
                EvaluationReport report = evaluationService.evaluate(dataset, provider);
                rows.add(new ModelScore(model, report.averageScores()));
            }
            return new ComparisonReport(dataset.name(), rows);
        } finally {
            modelRegistry.setModel(originalProvider, originalModelName);
        }
    }
}
