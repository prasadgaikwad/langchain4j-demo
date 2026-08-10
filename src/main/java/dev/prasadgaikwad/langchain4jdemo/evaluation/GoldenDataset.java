package dev.prasadgaikwad.langchain4jdemo.evaluation;

import java.util.List;

/**
 * A named collection of {@link GoldenQuestion}s covering one capability of the
 * demo (RAG over the sample documents, few-shot sentiment classification, or
 * plain chat). The bundled datasets are small so they run fast and fully
 * offline in tests; the expected answers are written against the sample
 * documents in {@code sample-data/}.
 *
 * @param name           the dataset name, used in reports and the {@code /eval} CLI command
 * @param goldenQuestions the question/answer pairs to evaluate
 */
public record GoldenDataset(String name, List<GoldenQuestion> goldenQuestions) {

    /** RAG questions answerable from the {@code sample-data/} documents. */
    public static GoldenDataset rag() {
        return new GoldenDataset("rag", List.of(
                new GoldenQuestion(
                        "How does LangChain4j keep conversation memory?",
                        "LangChain4j offers MessageWindowChatMemory and TokenWindowChatMemory."),
                new GoldenQuestion(
                        "How does semantic search find similar texts?",
                        "Semantic search embeds the query and finds stored vectors with the highest cosine similarity."),
                new GoldenQuestion(
                        "What is LangChain4j?",
                        "LangChain4j is a Java framework that simplifies building applications with LLMs.")
        ));
    }

    /** Sentiment classification examples for the few-shot classifier. */
    public static GoldenDataset sentiment() {
        return new GoldenDataset("sentiment", List.of(
                new GoldenQuestion("I absolutely loved this movie!", "POSITIVE"),
                new GoldenQuestion("This was the worst experience I have ever had.", "NEGATIVE"),
                new GoldenQuestion("The service was okay, nothing special.", "NEUTRAL")
        ));
    }

    /** General chat questions answerable without indexed documents. */
    public static GoldenDataset chat() {
        return new GoldenDataset("chat", List.of(
                new GoldenQuestion("What is 2 plus 2?", "4"),
                new GoldenQuestion("Is the Earth flat?", "No, the Earth is round.")
        ));
    }
}
