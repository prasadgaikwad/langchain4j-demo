package dev.prasadgaikwad.langchain4jdemo.chain;

import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.ai.Agent;
import org.springframework.stereotype.Service;

/**
 * Custom processing chain that composes deterministic stages with the
 * LLM-powered {@link Agent}.
 * <p>
 * Stage 1 (preprocess): the task is normalized and classified. Purely numeric
 * arithmetic expressions never reach the chat model; they are resolved locally
 * by the {@link CalculatorTool}.
 * <p>
 * Stage 2 (execute): everything else is delegated to the {@link Agent}, which
 * decides whether to call a tool or answer directly. This is the loop of
 * "reason, call tool, observe result" that LangChain4j drives under the hood.
 */
@Service
public class ChainService {

    private final Agent agent;
    private final CalculatorTool calculatorTool;

    public ChainService(Agent agent, CalculatorTool calculatorTool) {
        this.agent = agent;
        this.calculatorTool = calculatorTool;
    }

    public String ask(String memoryId, String task) {
        String normalized = task.trim();
        if (CalculatorTool.isArithmetic(normalized)) {
            return "Result: " + calculatorTool.calculate(normalized);
        }
        return agent.execute(memoryId, normalized);
    }
}
