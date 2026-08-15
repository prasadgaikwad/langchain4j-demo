package dev.prasadgaikwad.langchain4jdemo.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherToolTest {

    private final WeatherTool weatherTool = new WeatherTool();

    @Test
    void returnsCelsiusForKnownCity() {
        String result = weatherTool.getWeather(new WeatherTool.WeatherRequest("London",
                WeatherTool.TemperatureUnit.CELSIUS));

        assertThat(result).isEqualTo("It is 15.0 degrees celsius in London.");
    }

    @Test
    void convertsToFahrenheit() {
        String result = weatherTool.getWeather(new WeatherTool.WeatherRequest("London",
                WeatherTool.TemperatureUnit.FAHRENHEIT));

        assertThat(result).isEqualTo("It is 59.0 degrees fahrenheit in London.");
    }

    @Test
    void isCaseInsensitive() {
        String result = weatherTool.getWeather(new WeatherTool.WeatherRequest("tOkYo",
                WeatherTool.TemperatureUnit.CELSIUS));

        assertThat(result).contains("22.0 degrees celsius");
    }

    @Test
    void rejectsUnknownCity() {
        assertThatThrownBy(() -> weatherTool.getWeather(
                new WeatherTool.WeatherRequest("Atlantis", WeatherTool.TemperatureUnit.CELSIUS)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown city");
    }
}
