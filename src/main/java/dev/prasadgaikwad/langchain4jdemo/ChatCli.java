package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentSplitterType;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import dev.prasadgaikwad.langchain4jdemo.prompt.FewShotAssistant;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieExtractor;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieReview;
import dev.prasadgaikwad.langchain4jdemo.prompt.PromptService;
import dev.prasadgaikwad.langchain4jdemo.prompt.Sentiment;
import dev.prasadgaikwad.langchain4jdemo.prompt.TopicExtractor;
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
 * Interactive command-line interface combining conversation chat, semantic
 * search over embedded documents, RAG question answering, and a tool-using
 * agent. Disabled in tests via
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
    private final DocumentService documentService;
    private final ChainService chainService;
    private final PromptService promptService;
    private final FewShotAssistant fewShotAssistant;
    private final MovieExtractor movieExtractor;
    private final TopicExtractor topicExtractor;
    private final ConversationHistoryService historyService;
    private MemoryType currentMemoryType;

    public ChatCli(Assistant assistant,
                   QaService qaService,
                   ChatMemoryRegistry chatMemoryRegistry,
                   SemanticSearchService searchService,
                   DocumentService documentService,
                   ChainService chainService,
                   PromptService promptService,
                   FewShotAssistant fewShotAssistant,
                   MovieExtractor movieExtractor,
                   TopicExtractor topicExtractor,
                   ConversationHistoryService historyService) {
        this.assistant = assistant;
        this.qaService = qaService;
        this.chatMemoryRegistry = chatMemoryRegistry;
        this.searchService = searchService;
        this.documentService = documentService;
        this.chainService = chainService;
        this.promptService = promptService;
        this.fewShotAssistant = fewShotAssistant;
        this.movieExtractor = movieExtractor;
        this.topicExtractor = topicExtractor;
        this.historyService = historyService;
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

                String memoryId = currentMemoryType.memoryId(CONVERSATION_ID);
                String answer = assistant.chat(memoryId, input);
                historyService.record(memoryId, "user", input);
                historyService.record(memoryId, "ai", answer);
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
            case "/agent" -> runAgent(argument);
            case "/template" -> showTemplate(argument);
            case "/sentiment" -> classifySentiment(argument);
            case "/movie" -> extractMovie(argument);
            case "/topics" -> extractTopics(argument);
            case "/splitter" -> handleSplitterCommand(argument);
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

    private void handleSplitterCommand(String argument) {
        if (argument == null || argument.isBlank()) {
            System.out.println("Splitter type: " + documentService.splitterType().label());
            System.out.println("Max chunk size: " + documentService.maxChunkSize() + " chars");
            System.out.println("Max overlap   : " + documentService.maxOverlap() + " chars");
            return;
        }

        try {
            documentService.setSplitterType(DocumentSplitterType.fromLabel(argument.trim()));
            System.out.println("Switched splitter to '" + argument.trim()
                    + "'. Re-index documents to re-chunk them with the new strategy.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
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

        String memoryId = currentMemoryType.memoryId(CONVERSATION_ID);
        String answer = qaService.ask(memoryId, argument.trim());
        historyService.record(memoryId, "user", argument.trim());
        historyService.record(memoryId, "ai", answer);
        System.out.println("RAG > " + answer);
        System.out.println();
    }

    private void runAgent(String argument) {
        if (argument == null) {
            System.out.println("Usage: /agent <task>");
            return;
        }

        String answer = chainService.ask(currentMemoryType.memoryId(CONVERSATION_ID), argument.trim());
        System.out.println("Agent > " + answer);
        System.out.println();
    }

    private void showTemplate(String argument) {
        String movie = argument != null && !argument.isBlank() ? argument.trim() : "Inception";
        System.out.println("Rendered prompt template for \"" + movie + "\" (year 2010, enthusiastic tone):");
        System.out.println();
        System.out.println(promptService.renderMovieReviewPrompt(movie, 2010, "enthusiastic"));
        System.out.println();
    }

    private void classifySentiment(String argument) {
        if (argument == null) {
            System.out.println("Usage: /sentiment <text>");
            return;
        }

        Sentiment sentiment = fewShotAssistant.classify(argument.trim());
        System.out.println("Sentiment > " + sentiment);
        System.out.println();
    }

    private void extractMovie(String argument) {
        if (argument == null) {
            System.out.println("Usage: /movie <text>");
            return;
        }

        MovieReview review = movieExtractor.extract(argument.trim());
        System.out.println("Movie > " + review);
        System.out.println();
    }

    private void extractTopics(String argument) {
        if (argument == null) {
            System.out.println("Usage: /topics <text>");
            return;
        }

        List<String> topics = topicExtractor.extract(argument.trim());
        System.out.println("Topics > " + topics);
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
                  /index <file|directory>     Load and index documents into the embedding store (txt, md, pdf)
                  /search <query>             Semantic search over the indexed documents
                  /ask <question>             Answer the question using the indexed documents (RAG)
                  /agent <task>               Execute a task with the tool-using agent
                  /template [movie]           Render a prompt template (no API call)
                  /sentiment <text>           Classify sentiment with few-shot examples
                  /movie <text>               Extract structured movie data (output parser)
                  /topics <text>              Extract a list of topics (output parser)
                  /splitter                    Show the current document splitter
                  /splitter <type>             Switch splitter (recursive | paragraph | line | sentence | word | character)
                  /embed <text>               Embed a text and show its vector
                  /model                      Show the current embedding model
                  /model <name>               Switch embedding model (%s)
                  /store                      Show embedding store stats
                  /save [path]                Persist the embedding store
                  quit                        Exit the application

                REST API and WebSocket streaming are also available:
                see http://localhost:8080 and http://localhost:8080/chat.html
                """.formatted(KNOWN_EMBEDDING_MODELS));
    }
}
