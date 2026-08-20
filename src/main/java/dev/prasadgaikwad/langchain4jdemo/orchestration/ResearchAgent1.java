package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResearchAgent1 {

    @SystemMessage("You are a technical researcher. Research the given topic from a technical "
            + "perspective, focusing on implementation details, architecture, and best practices. "
            + "Return only the research findings and nothing else.")
    @UserMessage("""
            Research the following topic from a technical perspective.
            Return only the research findings and nothing else.

            Topic: {{topic}}
            """)
    @Agent(outputKey = "research1", description = "Researches a topic from a technical perspective")
    String research(@V("topic") String topic);
}
