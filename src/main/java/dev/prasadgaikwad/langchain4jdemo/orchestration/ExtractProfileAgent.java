package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ExtractProfileAgent {

    @SystemMessage("You are a profile extractor. Given a user prompt, extract a short profile "
            + "describing the person's role, interests, and expertise level. "
            + "Return only the profile and nothing else.")
    @UserMessage("""
            Extract a short profile from the following prompt.
            Return only the profile and nothing else.

            Prompt: {{prompt}}
            """)
    @Agent(outputKey = "profile", description = "Extracts a user profile (role, interests, expertise) from a prompt")
    String extractProfile(@V("prompt") String prompt);
}
