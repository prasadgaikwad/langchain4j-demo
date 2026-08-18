package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Third stage of the sequential blog-post pipeline: edits and polishes the
 * draft produced by {@link DraftAgent}. Reads the draft from the shared
 * {@code AgenticScope} key {@code "draft"} and writes the edited version
 * under {@code "edited"}.
 */
public interface EditorAgent {

    @SystemMessage("You are a professional editor. Edit and improve the following draft "
            + "for clarity, grammar, and flow. Fix any awkward phrasing and tighten "
            + "the prose. Return only the edited text and nothing else.")
    @UserMessage("""
            Edit and improve this blog post draft.
            Return only the edited text and nothing else.

            Draft:
            {{draft}}
            """)
    @Agent(outputKey = "edited", description = "Edits and polishes a blog post draft for clarity and flow")
    String editDraft(@V("draft") String draft);
}
