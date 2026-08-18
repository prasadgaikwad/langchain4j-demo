package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * First stage of the sequential blog-post pipeline: creates a structured
 * outline from a topic. The outline is written to the shared
 * {@code AgenticScope} under the key {@code "outline"} so the next agent
 * ({@link DraftAgent}) can consume it.
 */
public interface OutlineAgent {

    @SystemMessage("You are a blog post outline specialist. Create a clear, structured outline "
            + "with a title, introduction, 3-5 main sections, and a conclusion. "
            + "Return only the outline and nothing else.")
    @UserMessage("""
            Create a blog post outline for the following topic.
            Return only the outline and nothing else.

            Topic: {{topic}}
            """)
    @Agent(outputKey = "outline", description = "Creates a structured blog post outline from a topic")
    String createOutline(@V("topic") String topic);
}
