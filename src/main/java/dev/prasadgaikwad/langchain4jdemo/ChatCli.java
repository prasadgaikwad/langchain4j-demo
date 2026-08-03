package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Interactive command-line interface combining conversation chat with semantic
 * search over embedded documents. Disabled in tests via
 * {@code app.cli.enabled=false} so the context can load without blocking on stdin.
 */
@Component
@ConditionalOnProperty(name = "app.cli.enabled", havingValue = "true", matchIfMissing = true)
public class ChatCli implements CommandLineRunner {

    private static final String CONVERSATION_ID = "main";
    private static final String KNOWN_EMBEDDING_MODELS =
            "text-embedding-3-small | text-embedding-3-large | text-embedding-ada-002";

    private final Assistant assistant;
    private final QaService qaService;
    private final ChatMemoryRegistry chatMemoryRegistry;
    private final SemanticSearchService searchService;
    private MemoryType currentMemoryType;

    public ChatCli(Assistant assistant,
                   QaService qaService,
                   ChatMemoryRegistry chatMemoryRegistry,
                   SemanticSearchService searchService) {
        this.assistant = assistant;
        this.qaService = qaService;
        this.chatMemoryRegistry = chatMemoryRegistry;
        this.searchService = searchService;
        this.currentMemoryType = MemoryType.MESSAGE_WINDOW;
    }

    /**
     * Chats with the OpenAI GPT-4o-mini model, keeping the conversation history in
     * memory, and supports semantic search over embedded documents. See {@code /help}.
     */
    @Override
    public void run(String... args) {
        printHelp();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You > ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                if ("quit".equalsIgnoreCase(input)) {
                    System.out.println("Goodbye!");
                    break;
                }

                if (input.startsWith("/")) {
                    handleCommand(input);
                    continue;
                }

                String answer = assistant.chat(currentMemoryType.memoryId(CONVERSATION_ID), input);
                System.out.println("AI  > " + answer);
                System.out.println();
            }
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1] : null;

        switch (command) {
            case "/help" -> printHelp();
            case "/memory" -> handleMemoryCommand(argument);
            case "/clear" -> clearMemory();
            case "/index" -> index(argument);
            case "/search" -> search(argument);
            case "/ask" -> ask(argument);
            case "/embed" -> embed(argument);
            case "/model" -> handleModelCommand(argument);
            case "/store" -> printStoreStatus();
            case "/save" -> save(argument);
            default -> System.out.println("Unknown command: " + command + " (try /help)");
        }
    }

    private void handleMemoryCommand(String argument) {
        if (argument == null || argument.isBlank()) {
            printMemoryStatus();
            return;
        }

        try {
            currentMemoryType = MemoryType.fromLabel(argument.trim());
            System.out.println("Switched memory type to '" + currentMemoryType.label()
                    + "'. Conversation history was reset.");
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown memory type: " + argument.trim()
                    + " (use: message-window | token-window)");
        }
    }

    private void printMemoryStatus() {
        String memoryId = currentMemoryType.memoryId(CONVERSATION_ID);
        ChatMemory memory = chatMemoryRegistry.get(memoryId);
        int messageCount = memory == null ? 0 : memory.messages().size();
        System.out.println("Memory type  : " + currentMemoryType.label());
        System.out.println("Memory id    : " + memoryId);
        System.out.println("Messages kept: " + messageCount);
    }

    private void clearMemory() {
        String memoryId = currentMemoryType.memoryId(CONVERSATION_ID);
        ChatMemory memory = chatMemoryRegistry.get(memoryId);
        if (memory == null) {
            System.out.println("No conversation history yet.");
            return;
        }
        memory.clear();
        System.out.println("Conversation memory cleared.");
    }

    private void index(String argument) {
        if (argument == null) {
            System.out.println("Usage: /index <file-or-directory>");
            return;
        }

        Path path = Path.of(argument.trim());
        int indexed;
        if (Files.isDirectory(path)) {
            indexed = searchService.indexDirectory(path);
        } else if (Files.isRegularFile(path)) {
            indexed = searchService.indexDocument(path);
        } else {
            System.out.println("Path not found: " + path);
            return;
        }
        System.out.println("Indexed " + indexed + " segment(s). Store now holds "
                + searchService.storeSize() + " embedding(s).");
    }

    private void search(String argument) {
        if (argument == null) {
            System.out.println("Usage: /search <query>");
            return;
        }

        List<EmbeddingMatch<TextSegment>> matches = searchService.search(argument.trim());
        if (matches.isEmpty()) {
            System.out.println("No results. Index some documents first with /index <file-or-directory>.");
            return;
        }

        System.out.println("Top " + matches.size() + " results for: \"" + argument + "\"");
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            System.out.printf("%d. [score %.4f] %s%n", i + 1, match.score(), match.embedded().text());
        }
        System.out.println();
    }

    private void ask(String argument) {
        if (argument == null) {
            System.out.println("Usage: /ask <question>");
            return;
        }
        if (searchService.storeSize() == 0) {
            System.out.println("Embedding store is empty. Index some documents first with /index <file-or-directory>.");
            return;
        }

        String answer = qaService.ask(currentMemoryType.memoryId(CONVERSATION_ID), argument.trim());
        System.out.println("RAG > " + answer);
        System.out.println();
    }

    private void embed(String argument) {
        if (argument == null) {
            System.out.println("Usage: /embed <text>");
            return;
        }

        float[] vector = searchService.embed(argument.trim()).vector();
        System.out.println("Embedding (" + vector.length + " dimensions) of \"" + argument + "\":");
        System.out.println(shortPreview(vector));
        System.out.println();
    }

    private void handleModelCommand(String argument) {
        if (argument == null) {
            System.out.println("Embedding model: " + searchService.modelName());
            return;
        }

        String modelName = argument.trim();
        searchService.setEmbeddingModel(modelName);
        System.out.println("Switched embedding model to '" + modelName
                + "'. Re-index documents to embed them with the new model.");
    }

    private void printStoreStatus() {
        Path storePath = searchService.storePath();
        System.out.println("Embedding model : " + searchService.modelName());
        System.out.println("Store size      : " + searchService.storeSize());
        System.out.println("Store file      : " + (storePath != null ? storePath : "(none)"));
    }

    private void save(String argument) {
        Path path = argument != null ? Path.of(argument.trim()) : null;
        if (path != null && Files.isDirectory(path)) {
            System.out.println("Please provide a file path, not a directory.");
            return;
        }
        boolean saved = path != null ? searchService.save(path) : searchService.save();
        if (saved) {
            System.out.println("Embedding store saved.");
        } else {
            System.out.println("No store path configured; use /save <path>.");
        }
    }

    private String shortPreview(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        int preview = Math.min(8, vector.length);
        for (int i = 0; i < preview; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format(Locale.ROOT, "%.4f", vector[i]));
        }
        if (vector.length > preview) {
            sb.append(", ...");
        }
        return sb.append("]").toString();
    }

    private void printHelp() {
        System.out.println("""
                LangChain4j demo
                ----------------
                Type a question to chat with the AI, or use a command:
                  /help                       Show this help
                  /memory                     Show current memory type and state
                  /memory <type>              Switch memory type (message-window | token-window)
                  /clear                      Clear the current conversation memory
                  /index <file|directory>     Load and index documents into the embedding store
                  /search <query>             Semantic search over the indexed documents
                  /ask <question>             Answer the question using the indexed documents (RAG)
                  /embed <text>               Embed a text and show its vector
                  /model                      Show the current embedding model
                  /model <name>               Switch embedding model (%s)
                  /store                      Show embedding store stats
                  /save [path]                Persist the embedding store
                  quit                        Exit the application
                """.formatted(KNOWN_EMBEDDING_MODELS));
    }
}
