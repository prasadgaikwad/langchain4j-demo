package dev.prasadgaikwad.langchain4jdemo.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * A generic worker sub-agent of the {@link CrewService} supervisor. The
 * supervisor delegates the user request as the {@code task} argument, which the
 * framework passes to {@link #run(String)} by name (the JSON protocol of the
 * agentic planner).
 */
public interface CrewTaskAgent {

    @SystemMessage("You are a specialist agent. Complete the task delegated to you using the tools "
            + "available. Return the final answer and nothing else.")
    @UserMessage("You have been delegated the following task. Use the tools available to complete it.\n"
            + "Delegated task: {{task}}")
    @Agent
    String run(@V("task") String task);
}
