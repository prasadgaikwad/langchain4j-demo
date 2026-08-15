# 28 — Multimodal: vision

## Overview
`VisionService` sends an image together with a question to a multimodal chat
model. The image can be provided **by URL** or as **raw bytes** (base64 + mime),
matching the two ways OpenAI accepts image input. The model returns a text
description.

## Key concepts / API
- `ImageContent.from(URI)` — image by URL.
- `ImageContent.from(base64, mimeType)` — image by raw data.
- `UserMessage.from(TextContent.from(question), image)` — text + image in one
  user message (order of declaration = order sent).
- Multimodal AI service methods can also take `Content` / `ImageContent`
  parameters directly (→ 02).

## Code snippet
```java
// by URL
public String describeImage(String imageUrl, String question) {
    return describe(ImageContent.from(URI.create(imageUrl)), question);
}

// by raw bytes
public String describeImage(byte[] imageData, String mimeType, String question) {
    return describe(ImageContent.from(Base64.getEncoder().encodeToString(imageData), mimeType), question);
}

private String describe(ImageContent image, String question) {
    return chatModel.chat(ChatRequest.builder()
            .messages(List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(TextContent.from(question), image)))
            .build())
            .aiMessage().text();
}
```

## Diagram
```mermaid
flowchart LR
    URL[image URL] --> IC[ImageContent.from(URI)]
    B64[bytes + mime] --> IC2[ImageContent.from(base64, mime)]
    IC --> MSG[UserMessage(text + image)]
    IC2 --> MSG
    MSG --> LLM[multimodal ChatModel]
    LLM --> DESC[text description]
```

## Lessons learned / gotchas
- Base64 encoding is needed for raw data; always pass the correct mime type.
- Tests use a fake `ChatModel` and assert that the `ImageContent` actually
  reached the request — fully offline.
- The REST endpoint (`/api/describe`) accepts `imageUrl` or `imageData`+`mimeType`
  and returns 400 if neither is present.

## Related files
- `multimodal/VisionService.java`, `api/VisionApiController.java`,
  `api/DescribeRequest.java`, `api/DescribeResponse.java`, `ChatCli.java`
  (`/describe`), `src/test/java/dev/prasadgaikwad/langchain4jdemo/multimodal/*`.

## References
- https://docs.langchain4j.dev/tutorials/chat-and-language-models — Multimodality section
- https://docs.langchain4j.dev/tutorials/ai-services — `@UserMessage Content` parameters
