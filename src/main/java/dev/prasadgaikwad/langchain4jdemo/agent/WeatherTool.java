package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Custom tool that demonstrates a tool with an enum parameter: the
 * {@link TemperatureUnit} enum produces an enum schema the chat model must
 * fill in to call the tool. Parameters are flat (not a nested POJO) because
 * complex tool parameters bind unreliably across executors — see issue #245.
 * The weather data itself is fake and deterministic so the tool works offline.
 */
@Component
public class WeatherTool {

    private static final Map<String, Double> BASE_TEMPERATURES_CELSIUS = Map.of(
            "london", 15.0,
            "paris", 18.0,
            "tokyo", 22.0,
            "sydney", 12.0);

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
    public String getWeather(
            @P("The city name, e.g. London") String city,
            @P("The unit to return the temperature in") TemperatureUnit unit) {
        Double celsius = BASE_TEMPERATURES_CELSIUS.get(city.trim().toLowerCase(Locale.ROOT));
        if (celsius == null) {
            throw new IllegalArgumentException("Unknown city: " + city
                    + " (known cities: London, Paris, Tokyo, Sydney)");
        }
        double value = switch (unit) {
            case CELSIUS -> celsius;
            case FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0;
        };
        return String.format(Locale.ROOT, "It is %.1f degrees %s in %s.", value,
                unit.label(), city);
    }
}
