package dev.prasadgaikwad.langchain4jdemo.document;

import dev.langchain4j.data.segment.TextSegment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadAndSplitPrefixesEachSegmentWithTheFileName() throws Exception {
        Path file = tempDir.resolve("intro.txt");
        Files.writeString(file, "First paragraph about Java.\n\nSecond paragraph about Spring Boot.");

        DocumentService service = new DocumentService("recursive", 50, 10);

        List<TextSegment> segments = service.loadAndSplit(file);

        assertThat(segments).isNotEmpty();
        assertThat(segments).allSatisfy(segment -> assertThat(segment.text()).startsWith("intro.txt"));
    }

    @Test
    void smallerChunksProduceMoreSegments() throws Exception {
        Path file = tempDir.resolve("doc.txt");
        Files.writeString(file, "LangChain4j helps you build AI applications in Java. "
                + "It supports chat models, embeddings, and retrieval augmented generation. "
                + "The library integrates with Spring Boot.");

        DocumentService coarse = new DocumentService("recursive", 200, 0);
        DocumentService fine = new DocumentService("recursive", 30, 0);

        List<TextSegment> coarseSegments = coarse.loadAndSplit(file);
        List<TextSegment> fineSegments = fine.loadAndSplit(file);

        assertThat(coarseSegments).hasSize(1);
        assertThat(fineSegments).hasSizeGreaterThan(coarseSegments.size());
    }

    @Test
    void splitterLabelsMapToStrategies() {
        assertThat(DocumentSplitterType.fromLabel("line")).isEqualTo(DocumentSplitterType.LINE);
        assertThat(DocumentSplitterType.fromLabel("RECURSIVE")).isEqualTo(DocumentSplitterType.RECURSIVE);
        assertThat(DocumentSplitterType.fromLabel("character")).isEqualTo(DocumentSplitterType.CHARACTER);
        assertThatThrownBy(() -> DocumentSplitterType.fromLabel("bogus"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesPdfDocuments() throws Exception {
        Path pdf = tempDir.resolve("guide.pdf");
        createPdf(pdf, "Retrieval augmented generation lets a chat model answer from your own documents.");

        DocumentService service = new DocumentService("recursive", 200, 20);

        List<TextSegment> segments = service.loadAndSplit(pdf);

        assertThat(segments).isNotEmpty();
        assertThat(segments.get(0).text()).contains("Retrieval augmented generation");
    }

    @Test
    void loadAndSplitDirectorySkipsHiddenFiles() throws Exception {
        Path dir = tempDir.resolve("docs");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("visible.txt"), "Visible content about embeddings.");
        Files.writeString(dir.resolve(".DS_Store"), "garbage");

        DocumentService service = new DocumentService("recursive", 200, 20);

        List<TextSegment> segments = service.loadAndSplitDirectory(dir);

        assertThat(segments).isNotEmpty();
        assertThat(segments).allSatisfy(segment -> assertThat(segment.text()).startsWith("visible.txt"));
    }

    private static void createPdf(Path path, String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(path.toFile());
        }
    }
}
