package dev.prasadgaikwad.langchain4jdemo.evaluation;

import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a golden {@link GoldenDataset} through an {@link AnswerProvider} and
 * scores every answer with the configured {@link Metric}s, producing an
 * {@link EvaluationReport} with per-question scores and per-metric averages.
 * <p>
 * The default metric set mixes deterministic, offline metrics (exact match,
 * containment, F1, ROUGE-L) with an embedding-similarity metric that uses the
 * app's embedding model and an LLM-as-a-judge metric that uses the app's chat
 * model. In tests both models are fakes, so evaluation runs fully offline.
 */
@Service
public class EvaluationService {

    private final SemanticSearchService searchService;
    private final ChatModel judge;

    public EvaluationService(SemanticSearchService searchService, ChatModel judge) {
        this.searchService = searchService;
        this.judge = judge;
    }

    /**
     * Evaluates the dataset with the {@link #defaultMetrics()}.
     */
    public EvaluationReport evaluate(GoldenDataset dataset, AnswerProvider provider) {
        return evaluate(dataset, provider, defaultMetrics());
    }

    public EvaluationReport evaluate(GoldenDataset dataset, AnswerProvider provider, List<Metric> metrics) {
        List<EvaluationReportItem> items = new ArrayList<>();
        for (GoldenQuestion golden : dataset.goldenQuestions()) {
            String actual = provider.answer(golden.question());
            Map<String, Double> scores = new LinkedHashMap<>();
            for (Metric metric : metrics) {
                scores.put(metric.name(), round(metric.evaluate(golden.question(), golden.expectedAnswer(), actual)));
            }
            items.add(new EvaluationReportItem(golden.question(), golden.expectedAnswer(), actual, scores));
        }
        return new EvaluationReport(dataset.name(), List.copyOf(items), averages(items, metrics));
    }

    /**
     * The metrics the demo reports on: five deterministic/embedding metrics plus
     * the LLM-as-a-judge score.
     */
    public List<Metric> defaultMetrics() {
        return List.of(
                Metrics.exactMatch(),
                Metrics.contains(),
                Metrics.f1(),
                Metrics.rougeL(),
                Metrics.embeddingSimilarity(text -> searchService.embed(text).vector()),
                Metrics.judgeScore(judge));
    }

    private static Map<String, Double> averages(List<EvaluationReportItem> items, List<Metric> metrics) {
        Map<String, Double> averages = new LinkedHashMap<>();
        for (Metric metric : metrics) {
            double sum = 0.0;
            for (EvaluationReportItem item : items) {
                sum += item.scores().get(metric.name());
            }
            averages.put(metric.name(), round(items.isEmpty() ? 0.0 : sum / items.size()));
        }
        return averages;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
