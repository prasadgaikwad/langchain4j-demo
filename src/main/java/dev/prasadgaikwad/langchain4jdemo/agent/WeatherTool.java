package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Custom tool that demonstrates a tool with a structured parameter: the
 * {@link WeatherRequest} record produces a nested object schema (city as a
 * string, unit as an enum) that the chat model must fill in to call the tool.
 * The weather data itself is fake and deterministic so the tool works offline.
 */
@Component
public class WeatherTool {

    private static final Map<String, Double> BASE_TEMPERATURES_CELSIUS = Map.of(
            "london", 15.0,
            "paris", 18.0,
            "tokyo", 22.0,
            "sydney", 12.0);

    /**
     * Structured parameter for the tool; LangChain4j derives the object schema
     * from the record fields and the {@code unit} enum.
     */
    @Description("Weather request parameters")
    public record WeatherRequest(String city, TemperatureUnit unit) {
    }

    public enum TemperatureUnit {
        CELSIUS("celsius"),
        FAHRENHEIT("fahrenheit");

        private final String label;

        TemperatureUnit(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    @Tool("Gets the current temperature for a city, returning it in the requested unit")
    public String getWeather(WeatherRequest request) {
        Double celsius = BASE_TEMPERATURES_CELSIUS.get(request.city().trim().toLowerCase(Locale.ROOT));
        if (celsius == null) {
            throw new IllegalArgumentException("Unknown city: " + request.city()
                    + " (known cities: London, Paris, Tokyo, Sydney)");
        }
        double value = switch (request.unit()) {
            case CELSIUS -> celsius;
            case FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0;
        };
        return String.format(Locale.ROOT, "It is %.1f degrees %s in %s.", value,
                request.unit().label(), request.city());
    }
}
