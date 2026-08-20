package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TechnicalFormatAgent {

    @SystemMessage("You are a technical blog post formatter. Format the following draft "
            + "as a publish-ready technical blog post with proper Markdown headings, "
            + "code block placeholders where relevant, and a technical tone. "
            + "Return only the formatted blog post and nothing else.")
    @UserMessage("""
            Format this draft as a publish-ready technical blog post.
            Return only the formatted blog post and nothing else.

            Draft:
            {{draft}}
            """)
    @Agent(outputKey = "formatted", description = "Formats a draft as a technical blog post")
    String formatTechnical(@V("draft") String draft);
}
