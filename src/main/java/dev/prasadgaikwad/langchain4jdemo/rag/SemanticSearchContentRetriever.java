package dev.prasadgaikwad.langchain4jdemo.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;

import java.util.List;
import java.util.Map;

/**
 * A {@link ContentRetriever} that bridges the LangChain4j RAG pipeline with the
 * project's {@link SemanticSearchService}. Because it delegates to the service
 * at retrieval time, it always uses the currently selected embedding model and
 * store, even when the model is switched at runtime.
 * <p>
 * This is the "implement document retrieval" step of the RAG issue.
 */
public class SemanticSearchContentRetriever implements ContentRetriever {

    private final SemanticSearchService searchService;
    private final int maxResults;

    public SemanticSearchContentRetriever(SemanticSearchService searchService, int maxResults) {
        this.searchService = searchService;
        this.maxResults = maxResults;
    }

    @Override
    public List<Content> retrieve(Query query) {
        return searchService.search(query.text(), maxResults).stream()
                .map(this::toContent)
                .toList();
    }

    private Content toContent(EmbeddingMatch<TextSegment> match) {
        return Content.from(match.embedded(), Map.of(ContentMetadata.SCORE, match.score()));
    }
}
