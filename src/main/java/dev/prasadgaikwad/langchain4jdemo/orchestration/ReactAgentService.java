package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ReACT agent over a fresh, single-shot graph thread (no persistence). Runs the
 * task through LangGraph4j's {@link AgentExecutor}, collects the graph step
 * trace and answer, and returns them via {@link ReactResult}.
 *
 * <p>Graph construction and the stream/answer/trace helpers live in {@link
 * AgentGraphFactory} (shared by the stateful and human-in-the-loop variants,
 * issue T1).</p>
 */
@Service
public class ReactAgentService {

    private final CompiledGraph<AgentExecutor.State> compiledGraph;

    public ReactAgentService(AgentGraphFactory factory) throws GraphStateException {
        this.compiledGraph = factory.react();
    }

    public ReactResult run(String task) {
        AgentGraphFactory.GraphRun<AgentExecutor.State> run = AgentGraphFactory.stream(
                compiledGraph, Map.of("messages", UserMessage.from(task)), null);
        return new ReactResult(task, answerOf(run.lastState()), run.steps(), traceOf(run.lastState()));
    }

    /** Delegate retained for test and cross-service compatibility. */
    static String answerOf(AgentExecutor.State state) {
        return AgentGraphFactory.answerOf(state);
    }

    /** Delegate retained for test and cross-service compatibility. */
    static List<String> traceOf(AgentExecutor.State state) {
        return AgentGraphFactory.traceOf(state);
    }

    public record ReactResult(
            String task,
            String answer,
            List<String> steps,
            List<String> agentMessages
    ) {}
}
