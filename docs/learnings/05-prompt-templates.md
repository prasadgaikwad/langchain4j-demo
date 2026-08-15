# 05 — Prompt templates

## Overview
`PromptTemplate` renders reusable prompt skeletons with `{{placeholders}}` into
`ChatMessage`s. No LLM involved — a pure, offline-testable stage of prompt
engineering. `PromptService` uses it to build (and render, for display) the movie
review messages without calling a model.

## Key concepts / API
- `PromptTemplate.from("... {{var}} ...")` parses a template.
- `.apply(Map.of("var", value))` → `Prompt`.
- `Prompt.toSystemMessage()` / `toUserMessage()` → chat messages.
- Placeholders can also be filled with `{{it}}` in `@UserMessage`/`@SystemMessage`
  (→ 06).

## Code snippet
```java
Prompt systemPrompt = PromptTemplate.from(
        "You are a professional movie critic. Write in a {{tone}} tone.")
        .apply(Map.of("tone", tone));
Prompt userPrompt = PromptTemplate.from(
        "Write a short review for \"{{movie}}\" ({{year}}).")
        .apply(Map.of("movie", movie, "year", year));

List<ChatMessage> messages = List.of(
        systemPrompt.toSystemMessage(),
        userPrompt.toUserMessage());
```

## Diagram
```mermaid
flowchart LR
    T[Template text\n{{movie}} {{year}} {{tone}}] --> P[PromptTemplate]
    V[Map of values] --> P
    P -->|apply| PROMPT[Prompt]
    PROMPT -->|toSystemMessage| S[SystemMessage]
    PROMPT -->|toUserMessage| U[UserMessage]
```

## Lessons learned / gotchas
- Templates render **offline** — a great way to show exactly what will be sent to
  the model (the CLI `/template` command and the `/api/template` endpoint do this).
- `{{var}}` placeholders keep prompts data-driven instead of string-concatenated.
- ChatMessage typing: `SystemMessage.text()` vs `UserMessage.singleText()` differ;
  `PromptService.textOf` switches on message type.

## Related files
- `prompt/PromptService.java`, `api/PromptApiController.java`, `ChatCli.java`
  (`/template`).

## References
- https://docs.langchain4j.dev/tutorials/ai-services — `@UserMessage` template variables section
- https://docs.langchain4j.dev/tutorials/structured-outputs
