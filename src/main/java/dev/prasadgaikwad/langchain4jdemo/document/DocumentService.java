package dev.prasadgaikwad.langchain4jdemo.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.Document.FILE_NAME;

/**
 * Loads, parses, and splits documents into {@link TextSegment}s.
 * <p>
 * Files are parsed with a parser matching their format (PDFs via
 * {@link ApachePdfBoxDocumentParser}, everything else as plain text) and split
 * using the configured {@link DocumentSplitterType}. Each segment is prefixed
 * with its source file name, which improves retrieval quality.
 */
@Service
public class DocumentService {

    private final int maxChunkSize;
    private final int maxOverlap;
    private DocumentSplitterType splitterType;

    public DocumentService(@Value("${app.document.splitter:recursive}") String splitterType,
                           @Value("${app.document.max-chunk-size:200}") int maxChunkSize,
                           @Value("${app.document.max-overlap:20}") int maxOverlap) {
        this.splitterType = DocumentSplitterType.fromLabel(splitterType);
        this.maxChunkSize = maxChunkSize;
        this.maxOverlap = maxOverlap;
    }

    /**
     * Loads and splits a single file.
     *
     * @return the resulting text segments
     */
    public List<TextSegment> loadAndSplit(Path filePath) {
        return split(FileSystemDocumentLoader.loadDocument(filePath, parserFor(filePath)));
    }

    /**
     * Loads and splits all regular, non-hidden files in a directory.
     *
     * @return the resulting text segments
     */
    public List<TextSegment> loadAndSplitDirectory(Path directoryPath) {
        try (Stream<Path> files = Files.list(directoryPath)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> !file.getFileName().toString().startsWith("."))
                    .flatMap(file -> loadAndSplit(file).stream())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list documents in " + directoryPath, e);
        }
    }

    public void setSplitterType(DocumentSplitterType splitterType) {
        this.splitterType = splitterType;
    }

    public DocumentSplitterType splitterType() {
        return splitterType;
    }

    public int maxChunkSize() {
        return maxChunkSize;
    }

    public int maxOverlap() {
        return maxOverlap;
    }

    private List<TextSegment> split(Document document) {
        String fileName = document.metadata().getString(FILE_NAME);
        return splitterType.create(maxChunkSize, maxOverlap).split(document).stream()
                .map(segment -> withFileNamePrefix(segment, fileName))
                .toList();
    }

    private TextSegment withFileNamePrefix(TextSegment segment, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return segment;
        }
        return TextSegment.from(fileName + "\n" + segment.text(), segment.metadata());
    }

    private DocumentParser parserFor(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return new ApachePdfBoxDocumentParser();
        }
        return new TextDocumentParser();
    }
}
