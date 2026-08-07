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

## Running the Demo
1. Set your API key: `export OPENAI_API_KEY=...` (or configure `application.properties`)
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
/template [movie]           Render a prompt template (no API call)
/sentiment <text>           Classify sentiment with few-shot examples
/movie <text>               Extract structured movie data (output parser)
/topics <text>              Extract a list of topics (output parser)
/splitter                   Show the current document splitter
/splitter <type>            Switch splitter (recursive | paragraph | line | sentence | word | character)
/embed <text>               Embed a text and show its vector
/model                      Show the current embedding model
/model <name>               Switch embedding model
/store                      Show embedding store stats
/save [path]                Persist the embedding store
quit                        Exit the application
```

Try a semantic search with the bundled sample documents:

```
/index sample-data
/search what is a vector database?
/ask what does the document say about RAG?
/agent compute (20 * 5) - 8
/agent what do the documents say about agents?
/template Inception
/sentiment I absolutely loved this movie!
/movie Inception is a 2010 film directed by Christopher Nolan about dreams. It was great.
/topics RAG combines retrieval with generation to answer from your own documents
```

## Experiments Completed
1. **Memory and Context** — conversation memory with `@MemoryId`, switchable between `message-window` and `token-window` types
2. **Embeddings** — embedding generation, semantic search, switchable embedding models
3. **Document Processing** — PDF + text parsing, text splitting strategies, configurable chunk size/overlap
4. **RAG (Retrieval Augmented Generation)** — document retrieval + question-answering system
5. **Chains and Agents** — custom processing chain, specialized tool-using agent, `@Tool` implementations
6. **Prompting Techniques** — prompt templates, few-shot learning examples, output parsers / structured output

## Future Experiments and Features to Try

1. **LLM Integration**
   - Connect with different LLM providers (Anthropic, Google, local models via Ollama)
   - Experiment with different models (GPT-4, Claude)
   - Compare performance and capabilities

2. **Integration Features**
   - REST API endpoints
   - Streaming responses
   - WebSocket support
   - Database integration

3. **Evaluation and Testing**
   - Implement evaluation metrics
   - Create test suites for LLM responses
   - Benchmark different approaches

4. **Advanced Features**
   - Multi-modal capabilities
   - Structured output formatting
   - Advanced agent orchestration (LangChain4j `agentic` module)

## Getting Started
1. Clone the repository
2. Configure your API keys in `src/main/resources/application.properties` (or set `OPENAI_API_KEY` in your environment)
3. Run the application using Maven: `./mvnw spring-boot:run`

## Configuration

| Property                    | Default                 | Description                                  |
|-----------------------------|-------------------------|----------------------------------------------|
| `app.cli.enabled`           | `true`                  | Enable the interactive command-line chat CLI |
| `app.chat.model-name`       | `gpt-4o-mini`           | OpenAI model used for chat                   |
| `app.memory.max-messages`   | `10`                    | Max messages kept by `message-window` memory |
| `app.memory.max-tokens`     | `2000`                  | Max tokens kept by `token-window` memory     |
| `app.embedding.model-name`  | `text-embedding-3-small` | Embedding model used for indexing/search     |
| `app.embedding.store-path`  | `embedding-store.json`  | JSON file for persisting the embedding store |
| `app.embedding.max-results` | `5`                     | Default number of search results             |
| `app.rag.max-results`       | `5`                     | Chunks retrieved for each RAG question       |
| `app.document.splitter`     | `recursive`             | Document text splitting strategy             |
| `app.document.max-chunk-size` | `200`                 | Max chunk size in characters                 |
| `app.document.max-overlap`  | `20`                    | Overlap between consecutive chunks           |

## Current Dependency Versions
- Spring Boot 3.5.7
- LangChain4j BOM 1.18.1
- `langchain4j` and `langchain4j-open-ai` from the BOM
- `langchain4j-document-parser-apache-pdfbox` for PDF parsing

## Dependencies
- Spring Boot
- LangChain4j
- Additional dependencies based on features implemented

## Contributing
Feel free to fork and experiment with different features. Pull requests are welcome!
