package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * Collection-output AI Service. Returning {@code List<String>} makes LangChain4j
 * parse the model reply as a JSON array, an example of a structured output
 * beyond a single value.
 */
public interface TopicExtractor {

    @SystemMessage("""
            Extract the main topics from the given text.
            Return the result as a JSON array of strings, e.g. ["topic1", "topic2"].
            """)
    List<String> extract(@UserMessage String text);
}
