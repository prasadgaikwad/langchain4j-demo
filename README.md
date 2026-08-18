# LangChain4j Demo Project

This project demonstrates various features and capabilities of LangChain4j, a Java framework for building applications with Large Language Models (LLMs).

## What is LangChain4j?

LangChain4j is a Java framework that simplifies the development of applications powered by language models. It provides:

- Easy integration with various LLM providers (OpenAI, Anthropic, etc.)
- Tools for prompt engineering and management
- Built-in memory systems for maintaining conversation context
- Document loading and processing capabilities
- Embedding generation and vector store integration
- Agent and chain creation for complex workflows

## Resources

- [Official Documentation](https://docs.langchain4j.dev)
- [GitHub Repository](https://github.com/langchain4j/langchain4j)
- [Examples Repository](https://github.com/langchain4j/langchain4j-examples)
- [Maven Central](https://central.sonatype.com/artifact/dev.langchain4j/langchain4j)
- [Discord Community](https://discord.gg/KGZtZHZNJD)

## Learnings

Everything this demo taught us about LangChain4j — concepts, code snippets,
diagrams, and gotchas — is documented topic-by-topic in
[`docs/learnings/`](docs/learnings/README.md) (34 guides across app
architecture, AI services, prompts, structured output, memory, documents,
RAG, tools, agents, streaming, the web layer, multimodal, evaluation, model
comparison, offline testing, and chain-of-agents prompt chaining).

## Current Features
- Simple command-line interface to interact with user input
- Spring Boot integration
- Conversation memory via LangChain4j AI Services (`@MemoryId`)
- Two memory types, switchable at runtime:
  - `message-window`: sliding-window buffer limited by message count (`app.memory.max-messages`)
  - `token-window`: sliding window limited by token count, using the OpenAI tokenizer (`app.memory.max-tokens`)
- Embeddings and semantic search:
  - Index text files or directories into an `InMemoryEmbeddingStore` (persisted to JSON)
  - Semantic search over indexed documents with relevance scores
  - Switchable embedding models (`text-embedding-3-small`, `text-embedding-3-large`, `text-embedding-ada-002`)
- Document processing:
  - PDF loading and parsing (Apache PDFBox) plus plain-text files
  - Pluggable text splitting strategies (`recursive`, `paragraph`, `line`, `sentence`, `word`, `character`)
  - Configurable chunk size and overlap
- RAG (Retrieval Augmented Generation):
  - Question answering over your own documents via a `RetrievalAugmentor`
  - Retrieval of the most relevant chunks before each model call
- Chains and agents:
  - A tool-using agent (`AiServices` + `@Tool` methods) with chat memory
  - Tools: arithmetic calculator, document search, embedding-store stats
  - A custom processing chain that routes arithmetic locally and delegates everything else to the agent
- Prompting techniques:
  - Prompt templates (`PromptTemplate` with `{{placeholders}}`) rendered without an API call
  - Few-shot classification: labeled examples embedded in the system message
  - Output parsers / structured output: AI services returning an enum, a POJO record, or a `List<String>`
- Integration features:
  - REST API under `/api` (chat, RAG, agent, SSE streaming, sentiment, movie, topics, describe, template, index, search, store, history)
  - Streaming responses via Server-Sent Events (`/api/chat/stream`)
  - WebSocket streaming chat at `/ws/chat`
  - Database-backed conversation history (H2 via Spring Data JPA)
- Evaluation and testing:
  - Golden datasets (RAG, chat, sentiment) scored automatically
  - Metrics: exact match, containment, token F1, ROUGE-L, embedding similarity, LLM-as-a-judge
  - `/eval [rag|chat|sentiment]` prints a per-question and averaged report
- Advanced features:
  - Multi-modal: vision (describe an image by URL or base64), image generation (`gpt-image-1`), speech-to-text (`whisper-1`)
  - Function calling: custom tools with structured parameters (record + enum) and conversation-scoped state (`@ToolMemoryId`)
  - Dynamic tool selection: a `ToolProvider` exposes tools per request based on the task
  - Structured output at the model level: JSON schema response format constrains the reply to a record's schema
- LLM integration:
  - Multiple providers behind one interface: OpenAI, Anthropic, Google Gemini, and local models via Ollama
  - A `ModelRegistry` (a `ChatModel` bean) delegates every AI service to the selected `provider:model`, switchable at runtime with `/model chat <provider[:model]>`
  - Cross-model evaluation: `/eval compare [rag|chat|sentiment]` runs a golden dataset against every available model and prints a per-model averages table
- Advanced orchestration (LangChain4j `agentic` module):
  - A `CrewService`: a supervisor agent delegates tasks to typed `CrewTaskAgent` sub-agents (calculator, weather, document research), each bound to an existing `@Tool`; the delegated request is passed to the worker as its `task` argument, and the whole crew shares the switchable `ModelRegistry`
  - A `ChainOfAgentsService`: a sequential blog-post pipeline (Outline → Draft → Edit → Format) built with `AgenticServices.sequenceBuilder()`, where each typed sub-agent reads/writes to a shared `AgenticScope` and the full pipeline trace is exposed via REST
  - Streaming function calling: a streaming AI service streams tokens and can still call `@Tool` methods mid-stream

## Running the Demo
1. Set your API key(s): `export OPENAI_API_KEY=...`, `export ANTHROPIC_API_KEY=...`, `export GOOGLE_AI_GEMINI_API_KEY=...` (Ollama needs none) — or configure `application.properties`
2. Run: `./mvnw spring-boot:run`
3. Type a question to chat, or use a command:

```
/help                       Show help
/memory                     Show current memory type and state
/memory <type>              Switch memory type (message-window | token-window)
/clear                      Clear the current conversation memory
/index <file|directory>     Load and index documents into the embedding store (txt, md, pdf)
/search <query>             Semantic search over the indexed documents
/ask <question>             Answer the question using the indexed documents (RAG)
/agent <task>               Execute a task with the tool-using agent
/dynamic <task>             Execute a task with dynamically selected tools
/crew <task>                Execute a task with the agentic supervisor crew
/chain <topic>             Generate a blog post via a sequential chain of agents
/stream <task>              Stream a task with streaming function calling
/describe <url> [question]  Ask a multimodal model about an image
/generate <prompt>          Generate an image from a text prompt
/transcribe <file>          Transcribe an audio file to text
/schema <text>              Extract structured data via JSON schema
/template [movie]           Render a prompt template (no API call)
/sentiment <text>           Classify sentiment with few-shot examples
/movie <text>               Extract structured movie data (output parser)
/topics <text>              Extract a list of topics (output parser)
/splitter                   Show the current document splitter
/splitter <type>            Switch splitter (recursive | paragraph | line | sentence | word | character)
/embed <text>               Embed a text and show its vector
/model                      Show the current chat and embedding models
/model chat <provider[:model]>
                            Switch chat provider/model (e.g. anthropic, gemini:gemini-2.5-flash, ollama)
/model <name>               Switch embedding model
/store                      Show embedding store stats
/save [path]                Persist the embedding store
/eval [rag|chat|sentiment]  Run evaluation metrics over a golden dataset
/eval compare [rag|chat|sentiment]
                            Compare every available chat model on a golden dataset
quit                        Exit the application
```

Try a semantic search with the bundled sample documents:

```
/index sample-data
/search what is a vector database?
/ask what does the document say about RAG?
/agent compute (20 * 5) - 8
/agent what do the documents say about agents?
/crew what is 2 + 2?        (supervisor delegates to the calculator sub-agent)
/stream 12 * 12 = ?         (streams tokens while calling the calculator tool)
/template Inception
/sentiment I absolutely loved this movie!
/movie Inception is a 2010 film directed by Christopher Nolan about dreams. It was great.
/topics RAG combines retrieval with generation to answer from your own documents
```

## Web API

The same features are exposed over HTTP once the app is running (open
http://localhost:8080 for a summary page and http://localhost:8080/chat.html for an
SSE + WebSocket chat client).

| Method | Endpoint              | Description                                              |
|--------|-----------------------|----------------------------------------------------------|
| POST   | `/api/chat`           | Chat with the memory-backed assistant (`{message}`)      |
| POST   | `/api/ask`            | RAG question over indexed documents (`{message}`)       |
| POST   | `/api/agent`          | Delegate a task to the tool-using agent (`{message}`)   |
| POST   | `/api/chain`          | Run a sequential chain-of-agents pipeline (`{message}` topic) |
| GET    | `/api/chat/stream`    | Stream a chat reply over Server-Sent Events (`?message=&conversationId=`; each token is a JSON string) |
| POST   | `/api/sentiment`      | Sentiment classification (`{text}`)                      |
| POST   | `/api/movie`          | Extract structured movie data (`{text}`)                 |
| POST   | `/api/topics`         | Extract a topic list (`{text}`)                          |
| POST   | `/api/describe`       | Describe an image by URL or base64 (`{imageUrl|imageData, mimeType, question}`) |
| GET    | `/api/template`       | Render a prompt template (`?movie=`)                     |
| GET    | `/api/search`         | Semantic search over indexed documents (`?q=`)          |
| POST   | `/api/index`          | Index a file or directory (`?path=`; txt, md, pdf)      |
| GET    | `/api/store`          | Embedding store stats                                    |
| GET    | `/api/history`        | List all conversations                                   |
| GET    | `/api/history/{id}`   | Fetch the message history of one conversation            |
| DELETE | `/api/history/{id}`   | Delete a conversation's history                          |
| WS     | `/ws/chat`            | Stream chat tokens as frames, ending with `[DONE]`       |

Conversation history is persisted in an in-memory H2 database via Spring Data
JPA (`spring.datasource.*` below); the H2 console is available at
http://localhost:8080/h2-console. `/api/chat`, `/api/ask`, `/api/agent`, and
`/api/chat/stream` automatically record each user message and AI answer under
the request's `conversationId` (default `api`), so `/api/history/{id}` reflects
every conversation held through the REST API.

## Developer Tooling

- **Spring Boot DevTools** — automatic restart on code changes while developing
  (`spring-boot-devtools`, runtime scope)
- **Actuator** — monitoring endpoints at `/actuator/health`, `/actuator/info`,
  and `/actuator/metrics`; `info.app.*` provides the application metadata
- **Swagger UI** — browse and test every endpoint from the browser at
  http://localhost:8080/swagger-ui.html (OpenAPI JSON at `/v3/api-docs`)

## Experiments Completed
1. **Memory and Context** — conversation memory with `@MemoryId`, switchable between `message-window` and `token-window` types
2. **Embeddings** — embedding generation, semantic search, switchable embedding models
3. **Document Processing** — PDF + text parsing, text splitting strategies, configurable chunk size/overlap
4. **RAG (Retrieval Augmented Generation)** — document retrieval + question-answering system
5. **Chains and Agents** — custom processing chain, specialized tool-using agent, `@Tool` implementations
6. **Prompting Techniques** — prompt templates, few-shot learning examples, output parsers / structured output
7. **Integration Features** — REST API, streaming (SSE), WebSocket chat, database-backed history (H2 + Spring Data JPA)
8. **Developer Tooling** — Spring Boot DevTools auto-restart, Actuator monitoring, Swagger UI for API testing
9. **Evaluation and Testing** — golden datasets, offline metrics (exact match, containment, F1, ROUGE-L, embedding similarity, LLM-as-a-judge), `/eval` CLI command
10. **Advanced Features** — multi-modal (vision, image generation, speech-to-text), function calling with structured tool parameters and dynamic `ToolProvider`, JSON-schema structured output
11. **LLM Integration** — OpenAI, Anthropic, Gemini, and Ollama behind a single switchable `ModelRegistry` (a `ChatModel` bean), runtime `/model chat` switching, cross-model `/eval compare`
12. **Advanced Orchestration** — a supervisor-based crew (`langchain4j-agentic`) that delegates to specialized tool-bound sub-agents, plus streaming function calling
13. **Chain of Agents** — a sequential prompt-chaining pipeline (Outline → Draft → Edit → Format) built with `AgenticServices.sequenceBuilder()`, exposed via CLI and REST with full trace output

## Getting Started
1. Clone the repository
2. Configure your API keys in `src/main/resources/application.properties` (or set `OPENAI_API_KEY` in your environment)
3. Run the application using Maven: `./mvnw spring-boot:run`

## Configuration

| Property                    | Default                 | Description                                  |
|-----------------------------|-------------------------|----------------------------------------------|
| `app.cli.enabled`           | `true`                  | Enable the interactive command-line chat CLI |
| `app.chat.provider`        | `openai`                | Default chat provider (`openai` \| `anthropic` \| `gemini` \| `ollama`) |
| `app.chat.model-name`       | `gpt-4o-mini`           | OpenAI model used for chat                   |
| `app.models.anthropic-model` | `claude-haiku-4-5-20251001` | Anthropic model used for chat            |
| `app.models.gemini-model`  | `gemini-2.5-flash`      | Google Gemini model used for chat            |
| `app.models.ollama-model`  | `llama3.2`              | Ollama model used for chat                   |
| `app.ollama.base-url`      | `http://localhost:11434` | Ollama server base URL                      |
| `app.memory.max-messages`   | `10`                    | Max messages kept by `message-window` memory |
| `app.memory.max-tokens`     | `2000`                  | Max tokens kept by `token-window` memory     |
| `app.embedding.model-name`  | `text-embedding-3-small` | Embedding model used for indexing/search     |
| `app.embedding.store-path`  | `embedding-store.json`  | JSON file for persisting the embedding store |
| `app.embedding.max-results` | `5`                     | Default number of search results             |
| `app.rag.max-results`       | `5`                     | Chunks retrieved for each RAG question       |
| `app.image.model-name`      | `gpt-image-1`           | OpenAI model used for image generation       |
| `app.stt.model-name`        | `whisper-1`             | OpenAI model used for speech-to-text         |
| `app.document.splitter`     | `recursive`             | Document text splitting strategy             |
| `app.document.max-chunk-size` | `200`                 | Max chunk size in characters                 |
| `app.document.max-overlap`  | `20`                    | Overlap between consecutive chunks           |
| `spring.datasource.url`    | `jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1` | JDBC URL for conversation history |
| `spring.jpa.hibernate.ddl-auto` | `create-drop`       | Hibernate schema generation for history     |
| `spring.h2.console.enabled` | `true`                  | Enable the H2 web console                   |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | Exposed actuator endpoints |
| `management.endpoint.health.show-details` | `always`      | Include health component details            |
| `management.info.env.enabled` | `true`                | Expose `info.*` properties via `/actuator/info` |

## Current Dependency Versions
- Spring Boot 4.1.0
- LangChain4j BOM 1.19.0
- `langchain4j` and `langchain4j-open-ai` from the BOM
- `langchain4j-anthropic`, `langchain4j-google-ai-gemini`, and `langchain4j-ollama` for multi-provider chat
- `langchain4j-agentic` (1.19.0-beta29) for the supervisor-based crew orchestration
- `langchain4j-document-parser-apache-pdfbox` for PDF parsing
- `spring-boot-starter-webmvc`, `spring-boot-starter-websocket`, `spring-boot-starter-data-jpa`, and H2 for the REST/streaming/WebSocket/DB integration
- `spring-boot-devtools`, `spring-boot-starter-actuator`, and `springdoc-openapi-starter-webmvc-ui` (3.1.0) for developer tooling

## Dependencies
- Spring Boot
- LangChain4j
- Additional dependencies based on features implemented

## Contributing
Feel free to fork and experiment with different features. Pull requests are welcome!
