package dev.prasadgaikwad.langchain4jdemo.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent AI Service whose tools are selected dynamically by a
 * {@link dev.langchain4j.service.tool.ToolProvider} at request time, rather
 * than fixed at build time like {@link Agent}. The provider exposes the
 * calculator and note tools always, and the weather tool only when the task is
 * about the weather.
 */
public interface DynamicAgent {

    @SystemMessage("""
            You are an agent that accomplishes the user's task using the available tools.
            The set of tools available to you may change depending on the task, so only
            use a tool when it is relevant and available. If the task does not need a tool,
            answer directly. Be concise.
            """)
    String execute(@MemoryId String memoryId, @UserMessage String task);
}
