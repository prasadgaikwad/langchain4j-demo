package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TopicEditorAgent {

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
