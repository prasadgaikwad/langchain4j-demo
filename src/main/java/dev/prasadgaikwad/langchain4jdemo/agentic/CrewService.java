package dev.prasadgaikwad.langchain4jdemo.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.prasadgaikwad.langchain4jdemo.agent.CalculatorTool;
import dev.prasadgaikwad.langchain4jdemo.agent.DocumentSearchTool;
import dev.prasadgaikwad.langchain4jdemo.agent.WeatherTool;
import dev.prasadgaikwad.langchain4jdemo.memory.ChatMemoryRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * A small multi-agent system built with the LangChain4j agentic module. A
 * supervisor agent receives the task and delegates to specialized worker
 * agents — calculator, weather, and document research — each bound to one of
 * the demo's existing {@code @Tool}s.
 * <p>
 * Every agent shares the single {@link ChatModel} bean (the
 * {@code ModelRegistry}), so switching provider/model with {@code /model chat}
 * also switches the whole crew.
 */
@Service
public class CrewService {

    private final SupervisorAgent supervisor;

    public CrewService(ChatModel chatModel,
                       ChatMemoryRegistry chatMemoryRegistry,
                       CalculatorTool calculatorTool,
                       WeatherTool weatherTool,
                       DocumentSearchTool documentSearchTool) {
        ChatMemoryProvider memoryProvider = memoryId -> {
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .id((String) memoryId)
                    .maxMessages(10)
                    .build();
            chatMemoryRegistry.register((String) memoryId, memory);
            return memory;
        };

        UntypedAgent calculatorAgent = buildAgent(
                "Calculator", "Useful for arithmetic and any kind of math. Delegate calculations here.",
                chatModel, calculatorTool);
        UntypedAgent weatherAgent = buildAgent(
                "Weather", "Useful for current weather in known cities. Delegate weather questions here.",
                chatModel, weatherTool);
        UntypedAgent researchAgent = buildAgent(
                "Research", "Useful for questions about the indexed documents. Delegate document questions here.",
                chatModel, documentSearchTool);

        this.supervisor = AgenticServices.supervisorBuilder()
                .name("Crew")
                .description("Coordinates the demo crew of specialized agents.")
                .supervisorContext("You are the crew supervisor. Decide which agent is best suited for the task "
                        + "and delegate to it. If no agent fits, answer directly and concisely.")
                .chatModel(chatModel)
                .chatMemoryProvider(memoryProvider)
                .subAgents(calculatorAgent, weatherAgent, researchAgent)
                .responseStrategy(SupervisorResponseStrategy.LAST)
                .maxAgentsInvocations(10)
                .build();
    }

    /**
     * Runs a task through the supervisor, which may delegate it to one of the
     * specialized sub-agents.
     */
    public String run(String task) {
        return supervisor.invoke(task);
    }

    private static UntypedAgent buildAgent(String name, String description, ChatModel chatModel, Object tool) {
        return AgenticServices.agentBuilder()
                .name(name)
                .description(description)
                .chatModel(chatModel)
                .tools(tool)
                .userMessageProvider(input -> {
                    if (input instanceof Map<?, ?> map && map.containsKey("task")) {
                        return String.valueOf(map.get("task"));
                    }
                    return String.valueOf(input);
                })
                .build();
    }
}
