package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.memory.ChatMemory;
import dev.prasadgaikwad.langchain4jdemo.ai.Assistant;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import dev.prasadgaikwad.langchain4jdemo.memory.MemoryType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Scanner;

/**
 * Interactive command-line chat interface. Disabled in tests via
 * {@code app.cli.enabled=false} so the context can load without blocking on stdin.
 */
@Component
@ConditionalOnProperty(name = "app.cli.enabled", havingValue = "true", matchIfMissing = true)
public class ChatCli implements CommandLineRunner {

    private static final String CONVERSATION_ID = "main";

    private final Assistant assistant;
    private final ChatMemoryRegistry chatMemoryRegistry;
    private MemoryType currentMemoryType;

    public ChatCli(Assistant assistant, ChatMemoryRegistry chatMemoryRegistry) {
        this.assistant = assistant;
        this.chatMemoryRegistry = chatMemoryRegistry;
        this.currentMemoryType = MemoryType.MESSAGE_WINDOW;
    }

    /**
     * Chats with the OpenAI GPT-4o-mini model, keeping the conversation history in
     * memory. The memory type and its limits can be inspected and changed at
     * runtime (see {@code /help}).
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

    private void printHelp() {
        System.out.println("""
                LangChain4j conversation-memory demo
                ------------------------------------
                Type a question to chat with the AI. Commands:
                  /help                 Show this help
                  /memory               Show current memory type and state
                  /memory <type>        Switch memory type (message-window | token-window)
                  /clear                Clear the current conversation memory
                  quit                  Exit the application
                """);
    }
}
