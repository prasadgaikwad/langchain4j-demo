package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * A sequential "chain of agents" pipeline that runs four typed sub-agents
 * one after another, each consuming the previous step's output via the
 * shared {@code AgenticScope}:
 * <ol>
 *   <li>{@link OutlineAgent} — creates a structured outline from a topic</li>
 *   <li>{@link DraftAgent} — writes a full draft following the outline</li>
 *   <li>{@link EditorAgent} — edits and polishes the draft</li>
 *   <li>{@link FormatAgent} — formats into a publish-ready Markdown post</li>
 * </ol>
 * <p>
 * Every agent shares the single {@link ChatModel} bean (the
 * {@code ModelRegistry}), so switching provider/model with {@code /model chat}
 * also switches the whole chain.
 */
@Service
public class ChainOfAgentsService {

    private final UntypedAgent pipeline;

    public ChainOfAgentsService(ChatModel chatModel) {
        OutlineAgent outlineAgent = AgenticServices.agentBuilder(OutlineAgent.class)
                .chatModel(chatModel)
                .build();

        DraftAgent draftAgent = AgenticServices.agentBuilder(DraftAgent.class)
                .chatModel(chatModel)
                .build();

        EditorAgent editorAgent = AgenticServices.agentBuilder(EditorAgent.class)
                .chatModel(chatModel)
                .build();

        FormatAgent formatAgent = AgenticServices.agentBuilder(FormatAgent.class)
                .chatModel(chatModel)
                .build();

        this.pipeline = AgenticServices.sequenceBuilder()
                .subAgents(outlineAgent, draftAgent, editorAgent, formatAgent)
                .outputKey("formatted")
                .build();
    }

    /**
     * Runs a topic through the full outline → draft → edit → format pipeline
     * and returns the final formatted blog post.
     */
    public String run(String topic) {
        return (String) pipeline.invoke(Map.of("topic", topic));
    }

    /**
     * Runs the pipeline and returns the full {@link ChainPipelineResult} with
     * every intermediate stage for inspection or API trace responses.
     */
    @SuppressWarnings("unchecked")
    public ChainPipelineResult runWithTrace(String topic) {
        ResultWithAgenticScope<String> result =
                pipeline.invokeWithAgenticScope(Map.of("topic", topic));
        return new ChainPipelineResult(
                topic,
                result.agenticScope().readState("outline", (String) null),
                result.agenticScope().readState("draft", (String) null),
                result.agenticScope().readState("edited", (String) null),
                result.result());
    }
}
