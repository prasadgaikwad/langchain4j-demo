package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GraphOfAgentsService {

    private final UntypedAgent pipeline;

    public GraphOfAgentsService(ChatModel chatModel) {
        ExtractProfileAgent extractProfile = AgenticServices.agentBuilder(ExtractProfileAgent.class)
                .chatModel(chatModel).build();

        TopicSuggestionAgent topicSuggestion = AgenticServices.agentBuilder(TopicSuggestionAgent.class)
                .chatModel(chatModel).build();

        OutlineAgent outline = AgenticServices.agentBuilder(OutlineAgent.class)
                .chatModel(chatModel).build();

        TopicDraftAgent draft = AgenticServices.agentBuilder(TopicDraftAgent.class)
                .chatModel(chatModel).build();

        EditorAgent editor = AgenticServices.agentBuilder(EditorAgent.class)
                .chatModel(chatModel).build();

        TopicWriteupAgent writeup = AgenticServices.agentBuilder(TopicWriteupAgent.class)
                .chatModel(chatModel).build();

        this.pipeline = AgenticServices.plannerBuilder()
                .subAgents(extractProfile, topicSuggestion, outline, draft, editor, writeup)
                .outputKey("writeup")
                .planner(GoalOrientedPlanner::new)
                .build();
    }

    public String run(String prompt) {
        return (String) pipeline.invoke(Map.of("prompt", prompt));
    }

    @SuppressWarnings("unchecked")
    public GraphPipelineResult runWithTrace(String prompt) {
        ResultWithAgenticScope<String> result =
                pipeline.invokeWithAgenticScope(Map.of("prompt", prompt));
        List<String> path = result.agenticScope().readState("agentPath", List.of());
        return new GraphPipelineResult(
                prompt,
                result.agenticScope().readState("profile", (String) null),
                result.agenticScope().readState("topic", (String) null),
                result.agenticScope().readState("outline", (String) null),
                result.agenticScope().readState("draft", (String) null),
                result.agenticScope().readState("edited", (String) null),
                result.result(),
                path);
    }
}
