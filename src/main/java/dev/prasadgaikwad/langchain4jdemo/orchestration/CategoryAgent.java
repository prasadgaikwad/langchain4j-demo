package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CategoryAgent {

    @SystemMessage("You are a topic classifier. Classify the given topic as either "
            + "'technical' or 'general'. A topic is 'technical' if it involves programming, "
            + "software architecture, DevOps, infrastructure, or engineering concepts. "
            + "Return ONLY the category word and nothing else.")
    @UserMessage("""
            Classify the following topic as 'technical' or 'general'.
            Return ONLY the category word and nothing else.

            Topic: {{topic}}
            """)
    @Agent(outputKey = "category", description = "Classifies a topic as 'technical' or 'general'")
    String classifyTopic(@V("topic") String topic);
}
