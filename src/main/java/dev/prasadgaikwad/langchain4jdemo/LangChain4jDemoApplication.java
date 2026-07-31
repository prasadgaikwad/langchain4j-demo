package dev.prasadgaikwad.langchain4jdemo;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class LangChain4jDemoApplication implements CommandLineRunner {

    static void main(String[] args) {
        SpringApplication.run(LangChain4jDemoApplication.class, args);
    }

    /**
     * A simple command-line application that interacts with the user,
     * takes a question as input, and provides a concise answer using
     * the OpenAI GPT-4o-mini model.
     */
    @Override
    public void run(String... args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Please enter your question (type 'quit' to exit): ");
                String question = scanner.nextLine();

                if ("quit".equalsIgnoreCase(question)) {
                    System.out.println("Goodbye!");
                    break;
                }

                String response = model.chat("You are an helpful assistant. " +
                        "Answer this question in very concise way, only in 2 sentences maximum. Question: " + question);
                System.out.println("Answer: " + response);
                System.out.println(); // Add a blank line for better readability
            }
        }
    }
}
