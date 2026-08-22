# 38 - LangGraph4j ReACT Agent Executor

## What was learned

LangGraph4j (1.8.20) provides an `AgentExecutor` that compiles into an explicit state graph
implementing the ReACT (Reason + Act) loop. Unlike LangChain4j's built-in tool-calling
orchestration, this gives you a first-class state machine with named nodes and transitions:

```
__START__ → agent (LLM decides tool calls) → action (tool execution) → agent → ... → __END__
```

### Key findings

1. **`AgentExecutor.builder().chatModel(model).toolsFromObject(tool1, ...).build()`** returns
   a `StateGraph<AgentExecutor.State>` — call `.compile()` to get the executable graph.

2. **`AgentExecutor.State` extends `MessagesState`** with a `FINAL_RESPONSE` sentinel node.
   After execution, `state.finalResponse()` returns `Optional<String>` with the final answer.

3. **`StateGraph.stream()` returns `AsyncGenerator<NodeOutput>`** — each output has a
   `.node()` name (e.g., `"agent"`, `"action"`, `"__START__"`, `"__END__"`) that you can
   trace to visualize the ReACT loop steps.

4. **`CompiledGraph.invoke()` returns `Optional<State>`** — the final state contains the
   full message history. You can filter for `AiMessage` to get all intermediate reasoning.

5. **The AgentExecutor is independent of LangChain4j's `@AiService`** — it wires directly
   to the `ChatModel` and tool specifications, giving you explicit control over the loop.

### ReACT loop lifecycle

```java
// 1. Build
var graph = AgentExecutor.builder()
    .chatModel(chatModel)
    .toolsFromObject(calculatorTool, weatherTool)
    .build();

// 2. Compile
var compiled = graph.compile();

// 3. Invoke with a user message
var result = compiled.invoke(Map.of("messages", UserMessage.from("What is 2+2?")));

// 4. Extract answer
String answer = result.get().finalResponse().orElse("No response");
```

### Why LangGraph4j over plain LangChain4j tool calling

- Explicit graph state — you can inspect every node transition, not just the final result
- Tools are registered as state graph nodes, making the execution traceable
- Foundation for more complex patterns: conditional branching, cycles, human-in-the-loop
- Works alongside LangChain4j's `ChatModel` — no vendor lock-in

## Pitfalls

- `toolsFromObject()` takes `Object...` — pass tools directly, not wrapped in lists
- `AsyncGenerator.forEachRemaining()` does not exist; iterate with a for-each loop or stream
- `AgentExecutor.State` messages include ALL intermediate steps, not just the final exchange

## Recommendation

Use LangGraph4j's AgentExecutor when you need an observable, traceable ReACT loop. It's ideal
for debugging multi-step reasoning and tool orchestration without resorting to low-level prompt
engineering. The graph-based approach naturally extends to more complex patterns (issue #236: stateful
pipelines, issue #237: human-in-the-loop).
