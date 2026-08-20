package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface QualityScorerAgent {

    @SystemMessage("You are a quality reviewer. Score the following blog post draft on a scale "
            + "from 0.0 to 1.0 based on clarity, engagement, structure, and completeness. "
            + "Return ONLY the numeric score and nothing else (e.g. just '0.7').")
    @UserMessage("""
            Score the following blog post draft from 0.0 to 1.0.
            Return ONLY the numeric score and nothing else.

            Draft:
            {{draft}}
            """)
    @Agent(outputKey = "score", description = "Scores a blog post draft quality from 0.0 to 1.0")
    double scoreDraft(@V("draft") String draft);
}
