package dev.prasadgaikwad.langchain4jdemo.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI Service interface for question answering over the indexed documents.
 * <p>
 * Unlike {@link Assistant}, this interface is wired with a
 * {@code RetrievalAugmentor}: before each call, the user question is embedded and
 * used to retrieve the most relevant document chunks, which are appended to the
 * user message so the chat model can answer from your own data.
 */
public interface QaAssistant {

    @SystemMessage("""
            You are a question-answering assistant that answers only from the provided context.
            Answer the question using the information supplied in the user message. If the context
            does not contain the answer, respond with "I don't know". Keep the answer concise.
            """)
    String ask(@MemoryId String memoryId, @UserMessage String question);
}
