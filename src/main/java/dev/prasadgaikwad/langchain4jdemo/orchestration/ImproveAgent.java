package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ImproveAgent {

    @SystemMessage("You are a blog post editor. Improve the following draft for clarity, "
            + "engagement, and structure. Fix any awkward phrasing and tighten the prose. "
            + "Return only the improved draft and nothing else.")
    @UserMessage("""
            Improve this blog post draft. Focus on clarity, engagement, and structure.
            Return only the improved draft and nothing else.

            Draft:
            {{draft}}
            """)
    @Agent(outputKey = "draft", description = "Improves a blog post draft for clarity and engagement")
    String improveDraft(@V("draft") String draft);
}
