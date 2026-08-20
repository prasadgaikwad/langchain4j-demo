package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WorkflowDraftAgent {

    @SystemMessage("You are a blog post writer. Write a clear, engaging draft that combines "
            + "technical research and real-world examples. Use short paragraphs and a "
            + "conversational tone. Return only the draft and nothing else.")
    @UserMessage("""
            Write a blog post draft based on the following topic and research.
            Return only the draft and nothing else.

            Topic: {{topic}}

            Research:
            {{research1}}

            {{research2}}
            """)
    @Agent(outputKey = "draft", description = "Writes a blog post draft from topic and combined research")
    String writeDraft(@V("topic") String topic,
                      @V("research1") String research1,
                      @V("research2") String research2);
}
