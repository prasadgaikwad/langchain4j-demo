# 40 - LangGraph4j Human-in-the-Loop

## What was learned

LangGraph4j's `CompileConfig.interruptBefore()` pauses the graph before a named node executes.
Combined with checkpoint persistence (learning #39), this implements a human approval gate:
the agent proposes an action, the graph pauses, a human reviews, and the graph resumes.

### Key findings

1. **`CompileConfig.builder().interruptBefore("action").build()`** — pause the graph every
   time it is about to enter the `action` node (tool execution in the AgentExecutor graph).

2. **Node labels come from `org.bsc.langgraph4j.agent.Agent`** constants: `AGENT_LABEL` =
   `"agent"`, `ACTION_LABEL` = `"action"`, `END_LABEL` = `"end"`. The AgentExecutor builds
   its graph with exactly these node names.

3. **Detecting a pause**: after `invoke()`/`stream()`, call
   `compiledGraph.stateOf(config)`. If `snapshot.next()` equals the interrupted node, the
   graph is paused waiting. Otherwise the run completed normally.

4. **Resuming**: call `compiledGraph.stream(Map.of(), config)` with the same thread ID and
   empty input — the graph continues from the saved checkpoint through the interrupted node.

5. **Interrupts require checkpointing** — the pause works by saving state at the interrupt
   point; without a `checkpointSaver` the resume has nothing to restore from.

### Approval loop lifecycle

```java
// Compile: pause before tool execution
var compiled = graph.compile(CompileConfig.builder()
        .checkpointSaver(new MemorySaver())
        .interruptBefore("action")
        .build());

var config = RunnableConfig.builder().threadId("session-1").build();

// 1. Start — runs until the agent proposes a tool call, then pauses
compiled.invoke(Map.of("messages", UserMessage.from(task)), config);
var snapshot = compiled.stateOf(config).get();
boolean paused = "action".equals(snapshot.next());  // true → awaiting approval

// 2. Human reviews snapshot.state().messages() — last AiMessage holds the proposal

// 3. Resume — executes the approved action, continues the loop
compiled.invoke(Map.of(), config);
```

### Why this matters

- **Safety gate** — block destructive or expensive tools until a human signs off
- **Compliance** — record who approved what, when, with what feedback
- **Debugging** — step through agent reasoning one action at a time
- **Mixed autonomy** — auto-approve cheap tools, escalate risky ones to humans

## Pitfalls

- `stateOf(config)` throws if no checkpoint exists for the thread — check `isPresent()` first
- After resuming, the agent may propose *another* tool call — the approval loop must iterate
  until `awaitingApproval` is false
- Rejecting is not built-in: to reject, either update the state with corrective feedback or
  simply abandon the session (checkpoints are scoped per thread)
- Every tool call triggers the interrupt — there is no per-tool filtering out of the box;
  implement selective gating via conditional edges on a custom graph instead
- **Read results from `lastStateOf(config)`, never re-`invoke()`** — a second invoke with empty
  input re-runs the whole graph. The checkpoint already holds the final state.
- `AiMessage.text()` is null for pure tool-call messages — when surfacing the proposed action,
  fall back to the tool execution requests, not just the text.

## Recommendation

Use `interruptBefore(Agent.ACTION_LABEL)` with the AgentExecutor for a simple approve-everything
gate. For fine-grained control (approve only certain tools), build a custom `StateGraph` where
the pre-action node routes to an approval node conditionally based on the proposed tool name.
