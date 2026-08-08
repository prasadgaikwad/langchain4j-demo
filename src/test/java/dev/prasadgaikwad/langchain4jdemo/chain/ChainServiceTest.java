package dev.prasadgaikwad.langchain4jdemo.chain;

import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.ai.Agent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChainServiceTest {

    private final CalculatorTool calculatorTool = new CalculatorTool();

    @Test
    void routesArithmeticToTheCalculatorWithoutTheAgent() {
        ChainService chain = new ChainService((memoryId, task) -> "agent called: " + task, calculatorTool);

        String answer = chain.ask("main", " 2 + 3 * 4 ");

        assertThat(answer).isEqualTo("Result: 14.0");
    }

    @Test
    void delegatesWordedTasksToTheAgent() {
        ChainService chain = new ChainService((memoryId, task) -> "agent handled \"" + task + "\"", calculatorTool);

        String answer = chain.ask("main", "What can you do?");

        assertThat(answer).isEqualTo("agent handled \"What can you do?\"");
    }

    @Test
    void passesTheMemoryIdThroughToTheAgent() {
        ChainService chain = new ChainService((memoryId, task) -> "memory=" + memoryId, calculatorTool);

        String answer = chain.ask("message-window:main", "hello");

        assertThat(answer).isEqualTo("memory=message-window:main");
    }
}
