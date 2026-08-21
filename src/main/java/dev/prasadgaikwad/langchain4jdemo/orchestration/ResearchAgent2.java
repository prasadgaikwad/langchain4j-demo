package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResearchAgent2 {

    @SystemMessage("You are a trend researcher. Research the given topic focusing on real-world "
            + "examples, use cases, and trending applications. "
            + "Return only the research findings and nothing else.")
    @UserMessage("""
            Research the following topic focusing on real-world examples and trends.
            Return only the research findings and nothing else.

            Topic: {{topic}}
            """)
    @Agent(outputKey = "research2", description = "Researches a topic from a real-world examples perspective")
    String research(@V("topic") String topic);
}
