package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Second stage of the sequential blog-post pipeline: writes a full draft
 * following the outline produced by {@link OutlineAgent}. Reads the outline
 * from the shared {@code AgenticScope} key {@code "outline"} and writes the
 * draft under {@code "draft"}.
 */
public interface DraftAgent {

    @SystemMessage("You are a blog post writer. Write a clear, engaging draft that follows "
            + "the provided outline. Use short paragraphs and a conversational tone. "
            + "Return only the draft and nothing else.")
    @UserMessage("""
            Write a blog post draft based on the following outline.
            Return only the draft and nothing else.

            Outline:
            {{outline}}
            """)
    @Agent(outputKey = "draft", description = "Writes a full blog post draft from an outline")
    String writeDraft(@V("outline") String outline);
}
