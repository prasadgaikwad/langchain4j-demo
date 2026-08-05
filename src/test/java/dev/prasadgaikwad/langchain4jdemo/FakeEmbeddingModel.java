package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * Deterministic, locally-computed embedding model used to avoid real API calls in tests.
 * Each text is mapped to a vector of character-bigram counts, so texts sharing substrings
 * score as more similar.
 */
public class FakeEmbeddingModel implements EmbeddingModel {

    public static final int DIM = 64;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        return Response.from(textSegments.stream()
                .map(segment -> Embedding.from(vector(segment.text())))
                .toList());
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return Response.from(Embedding.from(vector(textSegment.text())));
    }

    private static float[] vector(String text) {
        float[] vector = new float[DIM];
        String normalized = text.toLowerCase();
        for (int i = 0; i < normalized.length() - 1; i++) {
            int hash = (normalized.charAt(i) * 31 + normalized.charAt(i + 1)) & 0x7fffffff;
            vector[hash % DIM] += 1.0f;
        }
        return vector;
    }
}
