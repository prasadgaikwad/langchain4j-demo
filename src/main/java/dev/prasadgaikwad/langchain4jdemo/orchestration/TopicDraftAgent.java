package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TopicDraftAgent {

    @SystemMessage("You are a blog post writer. Write a clear, engaging draft that follows "
            + "the provided outline. Use short paragraphs and a conversational tone. "
            + "Return only the draft and nothing else.")
    @UserMessage("""
            Write a blog post draft based on the following topic and outline.
            Return only the draft and nothing else.

            Topic: {{topic}}

            Outline:
            {{outline}}
            """)
    @Agent(outputKey = "draft", description = "Writes a full blog post draft from a topic and outline")
    String writeDraft(@V("topic") String topic, @V("outline") String outline);
}
