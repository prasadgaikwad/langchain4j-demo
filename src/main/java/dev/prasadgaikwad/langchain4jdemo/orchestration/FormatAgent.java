package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Fourth and final stage of the sequential blog-post pipeline: formats the
 * edited content into a publish-ready blog post with proper Markdown headings,
 * emphasis, and structure. Reads from the shared {@code AgenticScope} key
 * {@code "edited"} and writes the final result under {@code "formatted"}.
 */
public interface FormatAgent {

    @SystemMessage("You are a blog post formatter. Take the edited content and format it "
            + "as a publish-ready blog post with proper Markdown headings, emphasis, "
            + "and structure. Return only the formatted blog post and nothing else.")
    @UserMessage("""
            Format this edited content as a publish-ready blog post.
            Return only the formatted blog post and nothing else.

            Edited content:
            {{edited}}
            """)
    @Agent(outputKey = "formatted", description = "Formats edited content into a publish-ready Markdown blog post")
    String formatPost(@V("edited") String edited);
}
