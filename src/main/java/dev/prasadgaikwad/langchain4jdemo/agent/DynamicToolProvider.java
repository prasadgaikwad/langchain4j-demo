package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dynamic tool provider: decides which tools are available for each request
 * based on the user message, instead of registering a fixed set at build time.
 * <p>
 * The calculator and note tools are always available; the weather tool is only
 * exposed when the task mentions the weather. This keeps the model's tool set
 * (and therefore its prompt tokens and the risk of misusing a tool) small.
 */
@Component
public class DynamicToolProvider implements ToolProvider {

    private final CalculatorTool calculatorTool;
    private final WeatherTool weatherTool;
    private final NoteTool noteTool;

    public DynamicToolProvider(CalculatorTool calculatorTool,
                               WeatherTool weatherTool,
                               NoteTool noteTool) {
        this.calculatorTool = calculatorTool;
        this.weatherTool = weatherTool;
        this.noteTool = noteTool;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        List<AiServiceTool> tools = new ArrayList<>();
        tools.addAll(ToolService.findTools(calculatorTool));
        tools.addAll(ToolService.findTools(noteTool));

        String userMessage = request.userMessage().singleText();
        if (userMessage != null
                && userMessage.toLowerCase(Locale.ROOT).contains("weather")) {
            tools.addAll(ToolService.findTools(weatherTool));
        }

        return ToolProviderResult.builder()
                .addAll(tools)
                .build();
    }
}
