# 08 — Structured output: enum return type

## Overview
Returning an **enum** from an AI service method tells LangChain4j to constrain
the model to one of the enum constants and parse the reply into the enum. This is
the simplest form of structured output.

## Key concepts / API
- Method return type = `SomeEnum` → the LLM must reply with one of the values.
- LangChain4j appends format instructions and parses the reply before returning.
- Enum values are also used as **output parsers for classification** (→ 07).

## Code snippet
```java
public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

public interface FewShotAssistant {
    @SystemMessage("... Reply with exactly one of these words.")
    Sentiment classify(@UserMessage String text);
}

// usage
Sentiment sentiment = fewShotAssistant.classify("I absolutely loved this movie!");
```

## Diagram
```mermaid
flowchart LR
    T[text] --> LLM
    LLM -->|"constrained reply"| R["POSITIVE"]
    R --> ENUM[Sentiment.POSITIVE]
```

## Lessons learned / gotchas
- Structured output works at the AI service level with **zero boilerplate** — no
  manual JSON parsing.
- If you need nested data or lists, switch to a record (→ 09) or JSON Schema (→ 10).
- For determinism, combine with few-shot examples; lowering temperature helps too.

## Related files
- `prompt/Sentiment.java`, `prompt/FewShotAssistant.java`,
  `api/PromptApiController.java`.

## References
- https://docs.langchain4j.dev/tutorials/structured-outputs — Enum return types
