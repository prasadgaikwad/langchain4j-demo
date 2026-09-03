package dev.prasadgaikwad.langchain4jdemo.evaluation;

import dev.langchain4j.data.segment.TextSegment;
import dev.prasadgaikwad.langchain4jdemo.FakeChatModel;
import dev.prasadgaikwad.langchain4jdemo.FakeEmbeddingModel;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MetricsTest {

    @Test
    void exactMatchIgnoresCaseAndPunctuation() {
        assertThat(Metrics.exactMatch().evaluate("q", "hello world", "Hello, World!")).isEqualTo(1.0);
        assertThat(Metrics.exactMatch().evaluate("q", "hello", "goodbye")).isEqualTo(0.0);
    }

    @Test
    void containsScoresTheExpectedFact() {
        Metric contains = Metrics.contains();
        assertThat(contains.evaluate("q", "MessageWindowChatMemory",
                "LangChain4j offers MessageWindowChatMemory and TokenWindowChatMemory.")).isEqualTo(1.0);
        assertThat(contains.evaluate("q", "vector database",
                "Embeddings capture semantic meaning.")).isEqualTo(0.0);
    }

    @Test
    void f1PenalizesMissingAndExtraTokens() {
        Metric f1 = Metrics.f1();
        assertThat(f1.evaluate("q", "the cat sat on the mat", "the cat sat on the mat")).isEqualTo(1.0);
        assertThat(f1.evaluate("q", "a b c", "a b")).isEqualTo(0.8);
        assertThat(f1.evaluate("q", "a b c", "x y z")).isEqualTo(0.0);
        assertThat(f1.evaluate("q", "", "")).isEqualTo(1.0);
    }

    @Test
    void rougeLIsOrderAware() {
        Metric rougeL = Metrics.rougeL();
        assertThat(rougeL.evaluate("q", "a b c d", "a b c d")).isEqualTo(1.0);
        assertThat(rougeL.evaluate("q", "a b c d", "d c b a")).isBetween(0.0, 1.0);
        assertThat(rougeL.evaluate("q", "a b c d", "a b c d")).isGreaterThan(
                rougeL.evaluate("q", "a b c d", "d c b a"));
        assertThat(rougeL.evaluate("q", "a b c d", "w x y z")).isEqualTo(0.0);
    }

    @Test
    void embeddingSimilarityRanksRelatedTextAboveUnrelated() {
        Function<String, float[]> embedder =
                text -> new FakeEmbeddingModel().embed(TextSegment.from(text)).content().vector();
        Metric metric = Metrics.embeddingSimilarity(embedder);

        double same = metric.evaluate("q", "semantic search", "semantic search");
        double related = metric.evaluate("q", "semantic search", "semantic search finds similar texts");
        double unrelated = metric.evaluate("q", "semantic search", "the weather is nice today");

        assertThat(same).isCloseTo(1.0, within(1e-6));
        assertThat(related).isGreaterThan(unrelated);
        assertThat(unrelated).isGreaterThanOrEqualTo(0.0);
        assertThat(metric.evaluate("q", "  ", "anything")).isEqualTo(0.0);
    }

    @Test
    void rougeLExactMatchShortCircuitsToFullScore() {
        Metric rougeL = Metrics.rougeL();

        assertThat(rougeL.evaluate("q", "a b c d", "a b c d")).isEqualTo(1.0);
        assertThat(rougeL.evaluate("q", "a b c d", "a b c d e f")).isLessThan(1.0);
        assertThat(rougeL.evaluate("q", "hello world", "goodbye there")).isEqualTo(0.0);
    }

    @Test
    void judgeScoreParsesTheRatingAndNormalizes() {
        assertThat(Metrics.judgeScore(new FakeChatModel("4")).evaluate("q", "expected", "actual")).isEqualTo(0.8);
        assertThat(Metrics.judgeScore(new FakeChatModel("5")).evaluate("q", "expected", "actual")).isEqualTo(1.0);
        assertThat(Metrics.judgeScore(new FakeChatModel("10")).evaluate("q", "expected", "actual")).isEqualTo(1.0);
        assertThat(Metrics.judgeScore(new FakeChatModel("no score")).evaluate("q", "expected", "actual")).isEqualTo(0.0);
    }

    @Test
    void judgeScoreAsksTheJudgeForARating() {
        FakeChatModel judge = new FakeChatModel("3");
        Metrics.judgeScore(judge).evaluate("What is RAG?", "Retrieval Augmented Generation", "RAG");

        assertThat(judge.lastSystemMessage()).containsIgnoringCase("strict evaluator");
        assertThat(judge.lastUserMessage())
                .contains("What is RAG?")
                .contains("Expected answer: Retrieval Augmented Generation")
                .contains("Produced answer: RAG");
    }
}
