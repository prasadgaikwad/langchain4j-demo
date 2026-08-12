package dev.prasadgaikwad.langchain4jdemo.evaluation;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory for the evaluation metrics used by {@link EvaluationService}.
 * <p>
 * The deterministic metrics (exact match, containment, F1, ROUGE-L, and
 * embedding similarity) run fully offline; the LLM-as-a-judge metric asks a
 * {@link ChatModel} to rate the answer on a 0-5 scale and normalizes it to
 * {@code [0, 1]}. All metrics return scores in {@code [0, 1]}.
 */
public final class Metrics {

    private Metrics() {
    }

    public static Metric exactMatch() {
        return new Metric() {
            @Override
            public String name() {
                return "exact";
            }

            @Override
            public double evaluate(String question, String expected, String actual) {
                return normalize(expected).equals(normalize(actual)) ? 1.0 : 0.0;
            }
        };
    }

    /**
     * Whether the expected answer appears inside the produced answer. Suitable
     * for free-form answers where any faithful phrasing that includes the key
     * fact should count.
     */
    public static Metric contains() {
        return new Metric() {
            @Override
            public String name() {
                return "contains";
            }

            @Override
            public double evaluate(String question, String expected, String actual) {
                String expectedNorm = normalize(expected);
                String actualNorm = normalize(actual);
                if (expectedNorm.isEmpty()) {
                    return 0.0;
                }
                return actualNorm.contains(expectedNorm) ? 1.0 : 0.0;
            }
        };
    }

    /**
     * Token-level F1 between the expected and actual answers: harmonic mean of
     * precision and recall over the shared words. Punishes both missing facts
     * (low recall) and hallucinated extra words (low precision).
     */
    public static Metric f1() {
        return new Metric() {
            @Override
            public String name() {
                return "f1";
            }

            @Override
            public double evaluate(String question, String expected, String actual) {
                List<String> expectedTokens = tokenize(expected);
                List<String> actualTokens = tokenize(actual);
                if (expectedTokens.isEmpty() && actualTokens.isEmpty()) {
                    return 1.0;
                }
                if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
                    return 0.0;
                }
                int overlap = overlapCount(expectedTokens, actualTokens);
                double precision = overlap / (double) actualTokens.size();
                double recall = overlap / (double) expectedTokens.size();
                if (precision + recall == 0.0) {
                    return 0.0;
                }
                return 2 * precision * recall / (precision + recall);
            }
        };
    }

    /**
     * ROUGE-L (F-measure) over word sequences: fraction of the longest common
     * subsequence shared by both answers. Order-aware, unlike {@link #f1()}.
     */
    public static Metric rougeL() {
        return new Metric() {
            @Override
            public String name() {
                return "rougeL";
            }

            @Override
            public double evaluate(String question, String expected, String actual) {
                List<String> expectedTokens = tokenize(expected);
                List<String> actualTokens = tokenize(actual);
                if (expectedTokens.isEmpty() && actualTokens.isEmpty()) {
                    return 1.0;
                }
                if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
                    return 0.0;
                }
                int lcs = lcsLength(expectedTokens, actualTokens);
                double recall = lcs / (double) expectedTokens.size();
                double precision = lcs / (double) actualTokens.size();
                if (precision + recall == 0.0) {
                    return 0.0;
                }
                return 2 * precision * recall / (precision + recall);
            }
        };
    }

    /**
     * Cosine similarity between the embedding vectors of the expected and actual
     * answers. Uses the app's configured embedding model (the fake one offline),
     * so semantically similar phrasings score high even when no words match.
     */
    public static Metric embeddingSimilarity(Function<String, float[]> embedder) {
        return new Metric() {
            @Override
            public String name() {
                return "embed";
            }

            @Override
            public double evaluate(String question, String expected, String actual) {
                if (expected == null || expected.isBlank() || actual == null || actual.isBlank()) {
                    return 0.0;
                }
                return cosine(embedder.apply(expected), embedder.apply(actual));
            }
        };
    }

    /**
     * LLM-as-a-judge metric: a {@link ChatModel} is asked to rate how faithful
     * the produced answer is to the expected answer on a 0-5 scale; the reply is
     * parsed and normalized to {@code [0, 1]}. The judge is the same chat model
     * used by the app (a fake one in tests).
     */
    public static Metric judgeScore(ChatModel judge) {
        return new Metric() {
            @Override
            public String name() {
                return "judge";
            }

            @Override
            public double evaluate(String question, String expected, String actual) {
                ChatResponse response = judge.chat(ChatRequest.builder()
                        .messages(List.of(
                                SystemMessage.from(JUDGE_SYSTEM_PROMPT),
                                UserMessage.from(
                                        "Question: " + question + "\n"
                                                + "Expected answer: " + expected + "\n"
                                                + "Produced answer: " + actual)))
                        .build());
                return parseJudgeScore(response.aiMessage().text()) / 5.0;
            }
        };
    }

    private static final String JUDGE_SYSTEM_PROMPT = """
            You are a strict evaluator. You will be given a question, the expected answer,
            and the answer produced by an AI system. Rate how well the produced answer
            captures the expected answer on a scale from 0 (completely wrong or missing)
            to 5 (perfect). Reply with only a single integer.
            """;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private static int parseJudgeScore(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        int score = Integer.parseInt(matcher.group());
        return Math.max(0, Math.min(5, score));
    }

    private static String normalize(String text) {
        String lowered = text.toLowerCase(Locale.ROOT);
        String lettersAndDigits = lowered.replaceAll("[^a-z0-9]+", " ").trim();
        return lettersAndDigits.replaceAll("\\s+", " ");
    }

    private static List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return List.of(normalized.split(" "));
    }

    private static int overlapCount(List<String> expected, List<String> actual) {
        List<String> actualCopy = new ArrayList<>(actual);
        int overlap = 0;
        for (String token : expected) {
            if (actualCopy.remove(token)) {
                overlap++;
            }
        }
        return overlap;
    }

    private static int lcsLength(List<String> a, List<String> b) {
        int[][] dp = new int[a.size() + 1][b.size() + 1];
        for (int i = 1; i <= a.size(); i++) {
            for (int j = 1; j <= b.size(); j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[a.size()][b.size()];
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        int dim = Math.min(a.length, b.length);
        for (int i = 0; i < dim; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
