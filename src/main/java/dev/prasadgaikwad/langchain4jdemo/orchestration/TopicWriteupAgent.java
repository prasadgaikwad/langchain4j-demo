package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TopicWriteupAgent {

    @SystemMessage("You are a blog post formatter. Combine the user profile, topic, outline, "
            + "and edited content into a personalized, publish-ready blog post with proper "
            + "Markdown headings, emphasis, and structure. "
            + "Return only the final blog post and nothing else.")
    @UserMessage("""
            Create a personalized blog post using the following:
            - User profile: {{profile}}
            - Topic: {{topic}}
            - Outline: {{outline}}
            - Edited content: {{edited}}

            Return only the final blog post and nothing else.
            """)
    @Agent(outputKey = "writeup", description = "Formats a personalized blog post from profile, topic, outline, and edited content")
    String createWriteup(@V("profile") String profile,
                         @V("topic") String topic,
                         @V("outline") String outline,
                         @V("edited") String edited);
}
