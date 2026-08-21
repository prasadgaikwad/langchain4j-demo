package dev.prasadgaikwad.langchain4jdemo.orchestration;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowOfAgentsService {

    private final UntypedAgent pipeline;

    public WorkflowOfAgentsService(ChatModel chatModel) {
        var researchAgent1 = AgenticServices.agentBuilder(ResearchAgent1.class)
                .chatModel(chatModel).build();
        var researchAgent2 = AgenticServices.agentBuilder(ResearchAgent2.class)
                .chatModel(chatModel).build();
        var draftAgent = AgenticServices.agentBuilder(WorkflowDraftAgent.class)
                .chatModel(chatModel).build();
        var qualityScorer = AgenticServices.agentBuilder(QualityScorerAgent.class)
                .chatModel(chatModel).build();
        var improveAgent = AgenticServices.agentBuilder(ImproveAgent.class)
                .chatModel(chatModel).build();
        var categoryAgent = AgenticServices.agentBuilder(CategoryAgent.class)
                .chatModel(chatModel).build();
        var technicalFormat = AgenticServices.agentBuilder(TechnicalFormatAgent.class)
                .chatModel(chatModel).build();
        var generalFormat = AgenticServices.agentBuilder(GeneralFormatAgent.class)
                .chatModel(chatModel).build();

        UntypedAgent parallelResearch = AgenticServices.<String>parallelBuilder()
                .subAgents(researchAgent1, researchAgent2)
                .outputKey("research")
                .output(agenticScope -> {
                    String r1 = agenticScope.readState("research1", "");
                    String r2 = agenticScope.readState("research2", "");
                    return r1 + "\n\n" + r2;
                })
                .build();

        UntypedAgent refinementLoop = AgenticServices.<String>loopBuilder()
                .subAgents(qualityScorer, improveAgent)
                .maxIterations(3)
                .exitCondition(scope -> scope.readState("score", 0.0) >= 0.8)
                .build();

        UntypedAgent conditionalFormatter = AgenticServices.<String>conditionalBuilder()
                .subAgents(
                        scope -> "technical".equals(scope.readState("category", "")),
                        technicalFormat)
                .subAgents(
                        scope -> !"technical".equals(scope.readState("category", "")),
                        generalFormat)
                .build();

        this.pipeline = AgenticServices.sequenceBuilder()
                .subAgents(parallelResearch, draftAgent, refinementLoop, categoryAgent)
                .subAgents(conditionalFormatter)
                .outputKey("formatted")
                .build();
    }

    @SuppressWarnings("unchecked")
    public WorkflowPipelineResult run(String topic) {
        ResultWithAgenticScope<String> result =
                pipeline.invokeWithAgenticScope(Map.of("topic", topic));
        Map<String, Object> scope = result.agenticScope().state();

        List<String> executedAgents = new ArrayList<>();
        result.agenticScope().agentInvocations().forEach(
                inv -> executedAgents.add(inv.agentName()));

        return new WorkflowPipelineResult(
                topic,
                (String) scope.get("research"),
                (String) scope.get("draft"),
                (String) scope.get("formatted"),
                ((Number) scope.getOrDefault("iterationCount", 0)).intValue(),
                (String) scope.get("category"),
                executedAgents);
    }
}
