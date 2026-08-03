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

## Running the Demo
1. Set your API key: `export OPENAI_API_KEY=...` (or configure `application.properties`)
2. Run: `./mvnw spring-boot:run`
3. Type a question to chat, or use a command:

```
/help                       Show help
/memory                     Show current memory type and state
/memory <type>              Switch memory type (message-window | token-window)
/clear                      Clear the current conversation memory
/index <file|directory>     Load and index documents into the embedding store
/search <query>             Semantic search over the indexed documents
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
```

## Future Experiments and Features to Try

1. **LLM Integration**
   - Connect with different LLM providers (OpenAI, Anthropic, etc.)
   - Experiment with different models (GPT-3.5, GPT-4, Claude)
   - Compare performance and capabilities

2. **Memory and Context**
   - Implement conversation memory
   - Try different memory types (Buffer, Summary, Vector)
   - Experiment with context windows

3. **Embeddings**
   - Create and store embeddings
   - Implement semantic search
   - Try different embedding models

4. **Document Processing**
   - PDF document loading and parsing
   - Text splitting strategies
   - Document question-answering

5. **Chains and Agents**
   - Build custom processing chains
   - Create specialized agents for specific tasks
   - Implement tools for agents to use

6. **RAG (Retrieval Augmented Generation)**
   - Set up vector databases
   - Implement document retrieval
   - Create question-answering systems

7. **Prompting Techniques**
   - Few-shot learning examples
   - Prompt templates
   - Output parsers

8. **Integration Features**
   - REST API endpoints
   - Streaming responses
   - WebSocket support
   - Database integration

9. **Evaluation and Testing**
   - Implement evaluation metrics
   - Create test suites for LLM responses
   - Benchmark different approaches

10. **Advanced Features**
    - Multi-modal capabilities
    - Function calling
    - Structured output formatting
    - Custom tools development

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

## Current Dependency Versions
- Spring Boot 3.5.7
- LangChain4j BOM 1.18.1
- `langchain4j` and `langchain4j-open-ai` from the BOM

## Dependencies
- Spring Boot
- LangChain4j
- Additional dependencies based on features implemented

## Contributing
Feel free to fork and experiment with different features. Pull requests are welcome!
