# 39 - LangGraph4j Stateful Pipeline

## What was learned

LangGraph4j's `CompiledGraph` accepts a `CompileConfig` with a `BaseCheckpointSaver` that
persists graph state after each node execution. Combined with `RunnableConfig.threadId`, this
gives you session-based conversation persistence without any external storage.

### Key findings

1. **`CompileConfig.builder().checkpointSaver(new MemorySaver()).build()`** — compile the
   graph with checkpointing enabled. Every `invoke()`/`stream()` call saves a checkpoint.

2. **`RunnableConfig.builder().threadId("session-123").build()`** — pass this to
   `invoke(input, config)` or `stream(input, config)` to scope checkpoints to a thread.

3. **`CompiledGraph.getStateHistory(config)`** returns `Collection<StateSnapshot<State>>` —
   each snapshot contains the graph node, state, and config at that checkpoint.

4. **`StateSnapshot`** extends `NodeOutput` — has `.node()`, `.state()`, `.config()`, and
   `.next()` (the next node that would execute).

5. **`MemorySaver`** is in-memory only — lost on restart. For production, use `FileSystemSaver`
   or a database-backed implementation.

### Session lifecycle

```java
// Compile with checkpointing
var saver = new MemorySaver();
var compiled = graph.compile(CompileConfig.builder().checkpointSaver(saver).build());

// Run with a thread ID
var config = RunnableConfig.builder().threadId("session-123").build();
compiled.invoke(Map.of("messages", UserMessage.from("Hello")), config);

// Later: retrieve history
var history = compiled.getStateHistory(config);
for (var snapshot : history) {
    System.out.println("Node: " + snapshot.node() + ", Messages: " + snapshot.state().messages().size());
}
```

### Why checkpointing matters

- **Resume after failure** — if a long-running pipeline crashes, replay from last checkpoint
- **Multi-turn conversations** — each turn builds on previous checkpoints within the same thread
- **Audit trail** — every intermediate state is recorded, enabling debugging and compliance
- **Human-in-the-loop** — pause at a checkpoint, inspect state, then resume (issue #237)

## Pitfalls

- `MemorySaver` is ephemeral — fine for demos, not for production
- `getStateHistory()` returns snapshots in reverse chronological order (newest first)
- Thread IDs must be unique per session — collisions overwrite checkpoints
- The `AgentExecutor.State` checkpoint includes ALL messages (user + AI + tool), which can grow large
- **Read the outcome with `lastStateOf(config)`, never a second `invoke()`** — invoking again with
  empty input re-runs the graph. `lastStateOf` returns the final `StateSnapshot` from the
  checkpoint without any execution.

## Recommendation

Use `MemorySaver` for demos and development. For production, implement a `FileSystemSaver`
backed by a persistent directory, or write a custom `BaseCheckpointSaver` backed by a database.
The checkpoint API is the foundation for human-in-the-loop patterns (issue #237).
