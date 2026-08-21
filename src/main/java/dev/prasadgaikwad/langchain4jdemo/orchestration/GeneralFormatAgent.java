package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GeneralFormatAgent {

    @SystemMessage("You are a general blog post formatter. Format the following draft "
            + "as a publish-ready blog post with proper Markdown headings, engaging "
            + "subheadings, and an accessible tone. "
            + "Return only the formatted blog post and nothing else.")
    @UserMessage("""
            Format this draft as a publish-ready blog post.
            Return only the formatted blog post and nothing else.

            Draft:
            {{draft}}
            """)
    @Agent(outputKey = "formatted", description = "Formats a draft as a general-audience blog post")
    String formatGeneral(@V("draft") String draft);
}
