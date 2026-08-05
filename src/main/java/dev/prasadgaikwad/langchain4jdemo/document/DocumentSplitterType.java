package dev.prasadgaikwad.langchain4jdemo.document;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;

import java.util.Arrays;
import java.util.Locale;

/**
 * Text splitting strategies supported by the demo, backed by LangChain4j
 * {@link DocumentSplitter} implementations.
 */
public enum DocumentSplitterType {

    /** Splits by paragraphs, falling back to lines, then sentences, then words. */
    RECURSIVE("recursive"),
    PARAGRAPH("paragraph"),
    LINE("line"),
    SENTENCE("sentence"),
    WORD("word"),
    CHARACTER("character");

    private final String label;

    DocumentSplitterType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Creates the corresponding {@link DocumentSplitter} for the given chunk size and overlap.
     */
    public DocumentSplitter create(int maxChunkSize, int maxOverlap) {
        return switch (this) {
            case RECURSIVE -> DocumentSplitters.recursive(maxChunkSize, maxOverlap);
            case PARAGRAPH -> new DocumentByParagraphSplitter(maxChunkSize, maxOverlap);
            case LINE -> new DocumentByLineSplitter(maxChunkSize, maxOverlap);
            case SENTENCE -> new DocumentBySentenceSplitter(maxChunkSize, maxOverlap);
            case WORD -> new DocumentByWordSplitter(maxChunkSize, maxOverlap);
            case CHARACTER -> new DocumentByCharacterSplitter(maxChunkSize, maxOverlap);
        };
    }

    public static DocumentSplitterType fromLabel(String label) {
        return Arrays.stream(values())
                .filter(type -> type.label.equals(label.trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown splitter: " + label.trim() + " (use: " + String.join(" | ", labels()) + ")"));
    }

    private static String[] labels() {
        return Arrays.stream(values()).map(DocumentSplitterType::label).toArray(String[]::new);
    }
}
