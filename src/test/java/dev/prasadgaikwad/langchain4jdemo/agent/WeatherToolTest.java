package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherToolTest {

    private final WeatherTool weatherTool = new WeatherTool();

    @Test
    void returnsCelsiusForKnownCity() {
        String result = weatherTool.getWeather("London", WeatherTool.TemperatureUnit.CELSIUS);

        assertThat(result).isEqualTo("It is 15.0 degrees celsius in London.");
    }

    @Test
    void convertsToFahrenheit() {
        String result = weatherTool.getWeather("London", WeatherTool.TemperatureUnit.FAHRENHEIT);

        assertThat(result).isEqualTo("It is 59.0 degrees fahrenheit in London.");
    }

    @Test
    void isCaseInsensitive() {
        String result = weatherTool.getWeather("tOkYo", WeatherTool.TemperatureUnit.CELSIUS);

        assertThat(result).contains("22.0 degrees celsius");
    }

    @Test
    void rejectsUnknownCity() {
        assertThatThrownBy(() -> weatherTool.getWeather("Atlantis", WeatherTool.TemperatureUnit.CELSIUS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown city");
    }

    /**
     * Regression test for issue #245: the tool must execute successfully from
     * the flat argument JSON that real LLMs emit, through the same
     * specification + executor machinery the frameworks use — not just via
     * direct method calls.
     */
    @Test
    void executesFromFlatLlmStyleArgumentsThroughTheDefaultExecutor() {
        ToolSpecification spec = toolSpecification();
        ToolExecutor executor = executorFor(spec);

        String result = executor.execute(ToolExecutionRequest.builder()
                .name(spec.name())
                .arguments("{\"city\": \"Tokyo\", \"unit\": \"CELSIUS\"}")
                .id("call_1")
                .build(), "test-memory");

        assertThat(result).isEqualTo("It is 22.0 degrees celsius in Tokyo.");
    }

    @Test
    void schemaExposesFlatParametersWithEnumValues() {
        String json = toolSpecification().toJson();

        // Flat top-level parameters, not nested under a request object.
        assertThat(json).contains("\"city\"");
        assertThat(json).contains("\"unit\"");
        assertThat(json).doesNotContain("\"request\"");

        // The model needs the valid enum values to call the tool reliably.
        assertThat(json).contains("CELSIUS");
        assertThat(json).contains("FAHRENHEIT");
    }

    private ToolSpecification toolSpecification() {
        Map<String, ToolSpecification> specs = ToolSpecifications.toolSpecificationsFrom(weatherTool)
                .stream()
                .collect(Collectors.toMap(ToolSpecification::name, s -> s));
        return specs.get("getWeather");
    }

    private ToolExecutor executorFor(ToolSpecification spec) {
        return new DefaultToolExecutor(weatherTool, methodFor(spec.name()));
    }

    private java.lang.reflect.Method methodFor(String name) {
        for (java.lang.reflect.Method method : weatherTool.getClass().getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalStateException("No method named " + name);
    }
}
