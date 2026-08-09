package dev.prasadgaikwad.langchain4jdemo.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One semantic search hit with its relevance score")
public record SearchResult(
        @Schema(description = "Cosine similarity between 0 and 1", example = "0.87")
        double score,
        @Schema(description = "The matching document text", example = "A vector database stores embeddings...")
        String text) {
}
