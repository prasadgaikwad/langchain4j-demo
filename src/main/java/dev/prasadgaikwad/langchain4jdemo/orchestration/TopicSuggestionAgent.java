package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TopicSuggestionAgent {

    @SystemMessage("You are a topic suggestion specialist. Given a user profile, "
            + "suggest a single blog post topic that would be most relevant and valuable "
            + "for that person. Return only the topic and nothing else.")
    @UserMessage("""
            Suggest a blog post topic for the following profile.
            Return only the topic and nothing else.

            Profile: {{profile}}
            """)
    @Agent(outputKey = "topic", description = "Suggests a relevant blog post topic based on a user profile")
    String suggestTopic(@V("profile") String profile);
}
