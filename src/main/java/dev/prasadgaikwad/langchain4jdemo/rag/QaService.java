package dev.prasadgaikwad.langchain4jdemo.rag;

import dev.prasadgaikwad.langchain4jdemo.ai.QaAssistant;
import org.springframework.stereotype.Service;

/**
 * Question-answering system: answers questions from the indexed documents by
 * combining retrieval (see {@link SemanticSearchContentRetriever}) with the
 * {@link QaAssistant} chat model.
 */
@Service
public class QaService {

    private final QaAssistant qaAssistant;

    public QaService(QaAssistant qaAssistant) {
        this.qaAssistant = qaAssistant;
    }

    public String ask(String memoryId, String question) {
        return qaAssistant.ask(memoryId, question);
    }
}
