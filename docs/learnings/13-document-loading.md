# 13 — Document loading & parsing

## Overview
`DocumentService` loads files from the filesystem, parses them with a format-
matching `DocumentParser`, and splits them into `TextSegment`s ready for embedding
(→ 15). PDFs use Apache PDFBox; everything else is plain text.

## Key concepts / API
- `FileSystemDocumentLoader.loadDocument(path, parser)` — load one file.
- `FileSystemDocumentLoader.loadDocuments(path, parser)` / `loadDocumentsRecursively` —
  load many.
- Parsers: `TextDocumentParser` (txt/md), `ApachePdfBoxDocumentParser` (pdf),
  plus others in separate modules (Tika, POI, Markdown…).
- `Document.metadata()` — e.g. `Document.FILE_NAME` is set by the loader.
- `DocumentSplitter.split(document)` → `List<TextSegment>` (→ 14).

## Code snippet
```java
public List<TextSegment> loadAndSplit(Path filePath) {
    return split(FileSystemDocumentLoader.loadDocument(filePath, parserFor(filePath)));
}

private DocumentParser parserFor(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".pdf") ? new ApachePdfBoxDocumentParser()
                                 : new TextDocumentParser();
}
```

## Diagram
```mermaid
flowchart LR
    F[file / directory] --> LOAD[FileSystemDocumentLoader]
    LOAD --> P{extension?}
    P -->|.pdf| PDF[ApachePdfBoxDocumentParser]
    P -->|else| TXT[TextDocumentParser]
    PDF --> D[Document + metadata]
    TXT --> D
    D --> SPLIT[split into TextSegments]
```

## Lessons learned / gotchas
- Directory loading skips hidden files (`.*`) and regular files only.
- PDF parsing pulls in the `langchain4j-document-parser-apache-pdfbox` module
  (declared in `pom.xml`).
- Keep the parser selection a pure function of the file extension — easy to extend
  with more formats.

## Related files
- `document/DocumentService.java`, `document/DocumentSplitterType.java`,
  `pom.xml` (pdfbox parser module), `api/EmbeddingApiController.java`
  (`/index`).

## References
- https://docs.langchain4j.dev/tutorials/rag — Document loaders & parsers section
