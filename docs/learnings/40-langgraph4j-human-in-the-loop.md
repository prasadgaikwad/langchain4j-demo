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

3. **Detecting a REAL approval gate** (issue #247): `snapshot.next()` equals the interrupted
   node on *every* agent turn — the AgentExecutor wires an **unconditional** edge
   `agent → action` (`Agent.java`), so tool-free runs park there too. A genuine gate requires
   the parked state AND a pending proposal: the last `AiMessage` must carry
   `ToolExecutionRequest`s (`aiMessage.hasToolExecutionRequests()`).

4. **Resuming**: call `compiledGraph.stream(null, config)` (or `invoke(null, config)`) with the
   same thread ID — a `null` input becomes `GraphInput.resume()`, which continues at the
   checkpoint's next node. **Any non-null input (even `Map.of()`) starts a brand-new run from
   the entrypoint**, re-invoking the LLM instead of executing approved tools.

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
var lastAi = /* last AiMessage in snapshot.state().messages() */;
boolean gate = "action".equals(snapshot.next())          // parked...
        && lastAi != null && lastAi.hasToolExecutionRequests(); // ...with a real proposal

// 2. Human reviews the proposal: render lastAi.toolExecutionRequests() (name + arguments)

// 3. Resume — executes the approved tools, continues the loop
compiled.invoke(null, config);
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
- **`stream(null, config)` vs `stream(Map.of(), config)`**: `null` resumes; an empty map
  starts a NEW run from the entrypoint (issue #247 — this caused infinite re-prompts)
- **Rejection must not resume the graph**: the unconditional `agent → action` edge means a
  resumed run would execute the unapproved tools anyway. Terminate the session in the result
  layer instead (or rewrite state via `updateState(config, values, asNode)` with care)
- Tool-free runs park too (unconditional edge) — auto-complete them silently with one
  `stream(null, config)`; the action node sees no tool requests and routes to END
- Every tool call triggers the interrupt — there is no per-tool filtering out of the box;
  implement selective gating via conditional edges on a custom graph instead
- **Read results from `lastStateOf(config)`, never re-`invoke()`** — a second invoke with empty
  input re-runs the whole graph. The checkpoint already holds the final state.
- `AiMessage.text()` is null for pure tool-call messages — when surfacing the proposed action,
  fall back to the tool execution requests, not just the text.
- The `action` node writes the sentinel `"no tool execution request found!"` into
  `agent_response` when it finishes without requests and without a prior final answer — filter
  that string before treating `finalResponse` as the user-facing answer

## Recommendation

Use `interruptBefore(Agent.ACTION_LABEL)` with the AgentExecutor for a simple approve-everything
gate. For fine-grained control (approve only certain tools), build a custom `StateGraph` where
the pre-action node routes to an approval node conditionally based on the proposed tool name.
