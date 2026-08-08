package dev.prasadgaikwad.langchain4jdemo.prompt;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Few-shot classification AI Service. The system message embeds labeled
 * examples (a mini dataset of text + sentiment pairs) that teach the model the
 * output format and the decision boundaries. The {@link Sentiment} return type
 * acts as the output parser: the reply is parsed into the enum constant.
 */
public interface FewShotAssistant {

    @SystemMessage("""
            You are a sentiment classifier. Classify the sentiment of the given text
            as one of: POSITIVE, NEGATIVE, NEUTRAL. Reply with exactly one of these words.

            Examples:
            Text: "I absolutely loved this movie, best film of the year!"
            Sentiment: POSITIVE

            Text: "This restaurant is terrible, the food was cold and the service rude."
            Sentiment: NEGATIVE

            Text: "The package arrived on time. Nothing more to say."
            Sentiment: NEUTRAL
            """)
    Sentiment classify(@UserMessage String text);
}
