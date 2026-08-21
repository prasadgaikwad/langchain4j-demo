package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.ai.DynamicAgent;
import dev.prasadgaikwad.langchain4jdemo.agentic.CrewService;
import dev.prasadgaikwad.langchain4jdemo.chain.ChainService;
import dev.prasadgaikwad.langchain4jdemo.db.ConversationHistoryService;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentService;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentSplitterType;
import dev.prasadgaikwad.langchain4jdemo.orchestration.ChainOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.GraphOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.orchestration.WorkflowOfAgentsService;
import dev.prasadgaikwad.langchain4jdemo.document.DocumentSplitterType;
import dev.prasadgaikwad.langchain4jdemo.embedding.SemanticSearchService;
import dev.prasadgaikwad.langchain4jdemo.evaluation.AnswerProvider;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationReport;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationReportItem;
import dev.prasadgaikwad.langchain4jdemo.evaluation.EvaluationService;
import dev.prasadgaikwad.langchain4jdemo.evaluation.GoldenDataset;
import dev.prasadgaikwad.langchain4jdemo.llm.ComparisonReport;
import dev.prasadgaikwad.langchain4jdemo.llm.ModelComparisonService;
import dev.prasadgaikwad.langchain4jdemo.llm.ModelRegistry;
import dev.prasadgaikwad.langchain4jdemo.llm.ModelScore;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import dev.prasadgaikwad.langchain4jdemo.multimodal.ImageGenerationService;
import dev.prasadgaikwad.langchain4jdemo.multimodal.SpeechToTextService;
import dev.prasadgaikwad.langchain4jdemo.multimodal.VisionService;
import dev.prasadgaikwad.langchain4jdemo.prompt.FewShotAssistant;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieExtractor;
import dev.prasadgaikwad.langchain4jdemo.prompt.MovieReview;
import dev.prasadgaikwad.langchain4jdemo.prompt.PromptService;
import dev.prasadgaikwad.langchain4jdemo.prompt.Sentiment;
import dev.prasadgaikwad.langchain4jdemo.prompt.TopicExtractor;
import dev.prasadgaikwad.langchain4jdemo.rag.QaService;
import dev.prasadgaikwad.langchain4jdemo.streaming.StreamingAgent;
import dev.prasadgaikwad.langchain4jdemo.structured.JsonSchemaExtractionService;
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
    private final EvaluationService evaluationService;
    private final VisionService visionService;
    private final ImageGenerationService imageGenerationService;
    private final SpeechToTextService speechToTextService;
    private final DynamicAgent dynamicAgent;
    private final JsonSchemaExtractionService jsonSchemaExtractionService;
    private final ModelRegistry modelRegistry;
    private final ModelComparisonService modelComparisonService;
    private final CrewService crewService;
    private final StreamingAgent streamingAgent;
    private final ChainOfAgentsService chainOfAgentsService;
    private final GraphOfAgentsService graphOfAgentsService;
    private final WorkflowOfAgentsService workflowOfAgentsService;
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
                   ConversationHistoryService historyService,
                   EvaluationService evaluationService,
                   VisionService visionService,
                   ImageGenerationService imageGenerationService,
                   SpeechToTextService speechToTextService,
                   DynamicAgent dynamicAgent,
                   JsonSchemaExtractionService jsonSchemaExtractionService,
                   ModelRegistry modelRegistry,
                   ModelComparisonService modelComparisonService,
                   CrewService crewService,
                   StreamingAgent streamingAgent,
                   ChainOfAgentsService chainOfAgentsService,
                   GraphOfAgentsService graphOfAgentsService,
                   WorkflowOfAgentsService workflowOfAgentsService) {
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
        this.evaluationService = evaluationService;
        this.visionService = visionService;
        this.imageGenerationService = imageGenerationService;
        this.speechToTextService = speechToTextService;
        this.dynamicAgent = dynamicAgent;
        this.jsonSchemaExtractionService = jsonSchemaExtractionService;
        this.modelRegistry = modelRegistry;
        this.modelComparisonService = modelComparisonService;
        this.crewService = crewService;
        this.streamingAgent = streamingAgent;
        this.chainOfAgentsService = chainOfAgentsService;
        this.graphOfAgentsService = graphOfAgentsService;
        this.workflowOfAgentsService = workflowOfAgentsService;
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
            case "/dynamic" -> runDynamicAgent(argument);
            case "/crew" -> runCrew(argument);
            case "/chain" -> runChain(argument);
            case "/graph" -> runGraph(argument);
            case "/workflow" -> runWorkflow(argument);
            case "/stream" -> runStreamingAgent(argument);
            case "/describe" -> describeImage(argument);
            case "/generate" -> generateImage(argument);
            case "/transcribe" -> transcribeAudio(argument);
            case "/schema" -> extractWithSchema(argument);
            case "/template" -> showTemplate(argument);
            case "/sentiment" -> classifySentiment(argument);
            case "/movie" -> extractMovie(argument);
            case "/topics" -> extractTopics(argument);
            case "/splitter" -> handleSplitterCommand(argument);
            case "/embed" -> embed(argument);
            case "/model" -> handleModelCommand(argument);
            case "/store" -> printStoreStatus();
            case "/save" -> save(argument);
            case "/eval" -> runEvaluation(argument);
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

    private void runDynamicAgent(String argument) {
        if (argument == null) {
            System.out.println("Usage: /dynamic <task>");
            return;
        }

        String answer = dynamicAgent.execute(currentMemoryType.memoryId(CONVERSATION_ID), argument.trim());
        System.out.println("Agent > " + answer);
        System.out.println();
    }

    private void runCrew(String argument) {
        if (argument == null) {
            System.out.println("Usage: /crew <task>");
            return;
        }

        String answer = crewService.run(argument.trim());
        System.out.println("Crew > " + answer);
        System.out.println();
    }

    private void runChain(String argument) {
        if (argument == null) {
            System.out.println("Usage: /chain <topic>");
            return;
        }

        System.out.println("Chain > Generating blog post for: \"" + argument.trim() + "\"...");
        System.out.println();
        var result = chainOfAgentsService.runWithTrace(argument.trim());
        System.out.println("=== Outline ===");
        System.out.println(result.outline());
        System.out.println();
        System.out.println("=== Draft ===");
        System.out.println(result.draft());
        System.out.println();
        System.out.println("=== Edited ===");
        System.out.println(result.edited());
        System.out.println();
        System.out.println("=== Formatted ===");
        System.out.println(result.formatted());
        System.out.println();
    }

    private void runWorkflow(String argument) {
        if (argument == null) {
            System.out.println("Usage: /workflow <topic>");
            return;
        }

        System.out.println("Workflow > Running parallel/loop/conditional pipeline for: \"" + argument.trim() + "\"...");
        System.out.println();
        var result = workflowOfAgentsService.run(argument.trim());
        System.out.println("=== Category ===");
        System.out.println(result.category());
        System.out.println();
        System.out.println("=== Research ===");
        System.out.println(result.research());
        System.out.println();
        System.out.println("=== Draft ===");
        System.out.println(result.draft());
        System.out.println();
        System.out.println("=== Refinements ===");
        System.out.println(result.refinementIterations() + " iteration(s)");
        System.out.println();
        System.out.println("=== Formatted ===");
        System.out.println(result.formatted());
        System.out.println();
    }

    private void runGraph(String argument) {
        if (argument == null) {
            System.out.println("Usage: /graph <prompt>");
            return;
        }

        System.out.println("Graph > Running GOAP planner for: \"" + argument.trim() + "\"...");
        System.out.println();
        var result = graphOfAgentsService.runWithTrace(argument.trim());
        System.out.println("=== Agent Path ===");
        System.out.println(String.join(" -> ", result.agentPath()));
        System.out.println();
        System.out.println("=== Profile ===");
        System.out.println(result.profile());
        System.out.println();
        System.out.println("=== Topic ===");
        System.out.println(result.topic());
        System.out.println();
        System.out.println("=== Outline ===");
        System.out.println(result.outline());
        System.out.println();
        System.out.println("=== Draft ===");
        System.out.println(result.draft());
        System.out.println();
        System.out.println("=== Edited ===");
        System.out.println(result.edited());
        System.out.println();
        System.out.println("=== Writeup ===");
        System.out.println(result.writeup());
        System.out.println();
    }

    private void runStreamingAgent(String argument) {
        if (argument == null) {
            System.out.println("Usage: /stream <task>");
            return;
        }

        TokenStream tokenStream = streamingAgent.chat(currentMemoryType.memoryId(CONVERSATION_ID), argument.trim());
        System.out.print("Stream > ");
        tokenStream
                .onPartialResponse(System.out::print)
                .onCompleteResponse(response -> {
                    System.out.println();
                    System.out.println();
                })
                .onError(error -> {
                    System.out.println();
                    System.out.println("Stream error: " + error.getMessage());
                })
                .start();
    }

    private void describeImage(String argument) {
        if (argument == null) {
            System.out.println("Usage: /describe <image-url> [question]");
            return;
        }

        String[] parts = argument.trim().split("\\s+", 2);
        String imageUrl = parts[0];
        String question = parts.length > 1 ? parts[1] : "Describe this image in one sentence.";
        String answer = visionService.describeImage(imageUrl, question);
        System.out.println("Vision > " + answer);
        System.out.println();
    }

    private void generateImage(String argument) {
        if (argument == null) {
            System.out.println("Usage: /generate <prompt>");
            return;
        }

        dev.langchain4j.data.image.Image image = imageGenerationService.generate(argument.trim());
        System.out.println("Image > " + describeImageResult(image));
        System.out.println();
    }

    private void transcribeAudio(String argument) {
        if (argument == null) {
            System.out.println("Usage: /transcribe <audio-file>");
            return;
        }

        Path path = Path.of(argument.trim());
        if (!Files.isRegularFile(path)) {
            System.out.println("File not found: " + path);
            return;
        }
        try {
            String mimeType = mimeTypeFor(path);
            byte[] bytes = Files.readAllBytes(path);
            String text = speechToTextService.transcribe(bytes, mimeType);
            System.out.println("Transcription > " + text);
        } catch (Exception e) {
            System.out.println("Transcription failed: " + e.getMessage());
        }
        System.out.println();
    }

    private void extractWithSchema(String argument) {
        if (argument == null) {
            System.out.println("Usage: /schema <text>");
            return;
        }

        MovieReview review = jsonSchemaExtractionService.extractMovie(argument.trim());
        System.out.println("JSON schema > " + review);
        System.out.println();
    }

    private static String mimeTypeFor(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (name.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (name.endsWith(".m4a")) {
            return "audio/mp4";
        }
        return "audio/wav";
    }

    private static String describeImageResult(dev.langchain4j.data.image.Image image) {
        if (image.url() != null) {
            return "generated: " + image.url();
        }
        if (image.base64Data() != null) {
            return "generated " + image.mimeType() + " image (" + image.base64Data().length() + " base64 chars)";
        }
        return "generated image (no URL or data returned)";
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
            System.out.println("Chat model     : " + modelRegistry.currentLabel());
            System.out.println("Embedding model: " + searchService.modelName());
            System.out.println();
            System.out.println("Usage: /model chat <provider[:model]>");
            System.out.println("       /model <embedding-model>");
            return;
        }

        String spec = argument.trim();
        if (spec.startsWith("chat ")) {
            switchChatModel(spec.substring("chat ".length()));
            return;
        }

        String providerPart = spec.split(":")[0].toLowerCase(Locale.ROOT);
        if (providerPart.equals("openai") || providerPart.equals("anthropic")
                || providerPart.equals("gemini") || providerPart.equals("ollama")) {
            switchChatModel(spec);
            return;
        }

        searchService.setEmbeddingModel(spec);
        System.out.println("Switched embedding model to '" + spec
                + "'. Re-index documents to embed them with the new model.");
    }

    private void switchChatModel(String spec) {
        try {
            modelRegistry.setModel(spec);
            System.out.println("Switched chat model to '" + modelRegistry.currentLabel()
                    + "'. Every AI service now uses this model.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            System.out.println("Usage: /model chat <provider[:model]> where provider is one of:");
            for (String entry : modelRegistry.modelList().keySet()) {
                System.out.println("  " + entry + "  (" + modelRegistry.modelList().get(entry) + ")");
            }
        }
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

    private void runEvaluation(String argument) {
        String mode = argument != null && !argument.isBlank() ? argument.trim().toLowerCase(Locale.ROOT) : "rag";
        String memoryId = currentMemoryType.memoryId(CONVERSATION_ID);

        boolean compare = mode.equals("compare");
        String datasetMode = compare && argument != null && !argument.trim().equalsIgnoreCase("compare")
                ? argument.trim().split("\\s+", 2)[1].toLowerCase(Locale.ROOT)
                : (compare ? "rag" : mode);

        GoldenDataset dataset;
        AnswerProvider provider;
        switch (datasetMode) {
            case "rag" -> {
                if (searchService.storeSize() == 0) {
                    System.out.println("Embedding store is empty. Index some documents first with /index <file-or-directory>.");
                    return;
                }
                dataset = GoldenDataset.rag();
                provider = question -> qaService.ask(memoryId, question);
            }
            case "chat" -> {
                dataset = GoldenDataset.chat();
                provider = question -> assistant.chat(memoryId, question);
            }
            case "sentiment" -> {
                dataset = GoldenDataset.sentiment();
                provider = question -> fewShotAssistant.classify(question).name();
            }
            default -> {
                System.out.println("Unknown evaluation: " + datasetMode
                        + " (use: rag | chat | sentiment | compare[ rag|chat|sentiment])");
                return;
            }
        }

        System.out.println("Evaluating " + dataset.goldenQuestions().size() + " sample(s) on the '" + dataset.name()
                + "' dataset...");
        if (compare) {
            runModelComparison(dataset, provider);
        } else {
            EvaluationReport report = evaluationService.evaluate(dataset, provider);
            printEvaluationReport(report);
        }
    }

    private void runModelComparison(GoldenDataset dataset, AnswerProvider provider) {
        List<String> models = modelRegistry.availableModels();
        if (models.isEmpty()) {
            System.out.println("No models available for comparison. Set an OPENAI_API_KEY, ANTHROPIC_API_KEY, "
                    + "or GOOGLE_AI_GEMINI_API_KEY, or start Ollama locally.");
            return;
        }
        System.out.println("Comparing " + models.size() + " model(s): " + String.join(", ", models) + "...");
        ComparisonReport report = modelComparisonService.compare(dataset, provider);
        printComparisonReport(report);
    }

    private void printComparisonReport(ComparisonReport report) {
        if (report.models().isEmpty()) {
            System.out.println("Comparison produced no rows.");
            return;
        }
        List<String> metricNames = report.models().get(0).scores().keySet().stream().toList();
        System.out.println();
        System.out.println("=== Model comparison: " + report.dataset() + " ===");
        System.out.println("Model" + " ".repeat(Math.max(1, 38 - "Model".length()))
                + String.join("  ", metricNames));
        for (ModelScore row : report.models()) {
            System.out.println(row.model() + " ".repeat(Math.max(1, 38 - row.model().length()))
                    + metricNames.stream()
                            .map(metric -> String.format(Locale.ROOT, "%.2f",
                                    row.scores().getOrDefault(metric, 0.0)))
                            .collect(java.util.stream.Collectors.joining("  ")));
        }
        System.out.println();
    }

    private void printEvaluationReport(EvaluationReport report) {
        List<String> metricNames = report.items().isEmpty()
                ? report.averageScores().keySet().stream().toList()
                : report.items().get(0).scores().keySet().stream().toList();

        System.out.println();
        System.out.println("=== Evaluation: " + report.dataset() + " ===");
        System.out.println("Metrics: " + String.join(", ", metricNames));
        System.out.println();
        for (int i = 0; i < report.items().size(); i++) {
            EvaluationReportItem item = report.items().get(i);
            System.out.printf("[%d] %s%n", i + 1, item.question());
            System.out.println("    Expected: " + item.expected());
            System.out.println("    Actual  : " + item.actual());
            System.out.println("    Scores  : " + formatScores(item.scores(), metricNames));
        }
        System.out.println();
        System.out.println("Average: " + formatScores(report.averageScores(), metricNames));
        System.out.println();
    }

    private String formatScores(java.util.Map<String, Double> scores, List<String> metricNames) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < metricNames.size(); i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(String.format(Locale.ROOT, "%s=%.2f", metricNames.get(i),
                    scores.getOrDefault(metricNames.get(i), 0.0)));
        }
        return sb.toString();
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
                  /dynamic <task>             Execute a task with dynamically selected tools
                  /crew <task>                Execute a task with the agentic supervisor crew
                  /chain <topic>             Generate a blog post via a sequential chain of agents
                   /graph <prompt>           Generate a personalized blog post via a goal-oriented agent graph
                   /workflow <topic>         Generate a blog post via parallel/loop/conditional workflow
                  /stream <task>              Stream a task with streaming function calling
                  /describe <url> [question]  Ask a multimodal model about an image
                  /generate <prompt>          Generate an image from a text prompt
                  /transcribe <file>          Transcribe an audio file to text
                  /schema <text>              Extract structured data via JSON schema
                  /template [movie]           Render a prompt template (no API call)
                  /sentiment <text>           Classify sentiment with few-shot examples
                  /movie <text>               Extract structured movie data (output parser)
                  /topics <text>              Extract a list of topics (output parser)
                  /splitter                    Show the current document splitter
                  /splitter <type>             Switch splitter (recursive | paragraph | line | sentence | word | character)
                  /embed <text>               Embed a text and show its vector
                  /model                      Show the current chat and embedding models
                  /model chat <p[:m]>         Switch chat provider/model (e.g. anthropic, gemini:gemini-2.5-flash, ollama)
                  /model <name>               Switch embedding model (%s)
                  /store                      Show embedding store stats
                  /save [path]                Persist the embedding store
                  /eval [rag|chat|sentiment]  Run evaluation metrics over a golden dataset
                  /eval compare [rag|chat|sentiment]
                                              Compare every available chat model on a golden dataset
                  quit                        Exit the application

                REST API and WebSocket streaming are also available:
                see http://localhost:8080 and http://localhost:8080/chat.html
                """.formatted(KNOWN_EMBEDDING_MODELS));
    }
}
