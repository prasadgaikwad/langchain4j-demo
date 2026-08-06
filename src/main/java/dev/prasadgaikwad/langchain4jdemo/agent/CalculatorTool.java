package dev.prasadgaikwad.langchain4jdemo.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Tool that evaluates arithmetic expressions, registered with the agent via
 * {@code AiServices}. The {@link Tool} annotation makes the {@code calculate}
 * method available to the chat model as a function; the model decides when to
 * call it and supplies the {@link P}-described parameters.
 */
@Component
public class CalculatorTool {

    private static final Pattern ARITHMETIC = Pattern.compile("^[0-9+\\-*/().\\s]+$");

    /**
     * Heuristic used by the chain to short-circuit purely numeric expressions
     * without invoking the chat model at all.
     */
    public static boolean isArithmetic(String expression) {
        return expression != null
                && ARITHMETIC.matcher(expression).matches()
                && expression.matches(".*\\d.*")
                && expression.matches(".*[+\\-*/].*");
    }

    @Tool("Calculates the result of an arithmetic expression using +, -, *, / and parentheses")
    public double calculate(@P("The arithmetic expression to evaluate, e.g. \"(1 + 2) * 3\"") String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression must not be empty");
        }
        return new Evaluator(expression).evaluate();
    }

    private static final class Evaluator {

        private final String input;
        private int pos = 0;

        Evaluator(String input) {
            this.input = input.replaceAll("\\s+", "").trim();
        }

        double evaluate() {
            double value = expression();
            if (pos < input.length()) {
                throw new IllegalArgumentException("Unexpected character at position " + pos);
            }
            return value;
        }

        private double expression() {
            double value = term();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '+') {
                    pos++;
                    value += term();
                } else if (c == '-') {
                    pos++;
                    value -= term();
                } else {
                    break;
                }
            }
            return value;
        }

        private double term() {
            double value = factor();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '*') {
                    pos++;
                    value *= factor();
                } else if (c == '/') {
                    pos++;
                    double divisor = factor();
                    if (divisor == 0) {
                        throw new IllegalArgumentException("Division by zero");
                    }
                    value /= divisor;
                } else {
                    break;
                }
            }
            return value;
        }

        private double factor() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of expression");
            }
            char c = input.charAt(pos);
            if (c == '(') {
                pos++;
                double value = expression();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                pos++;
                return value;
            }
            if (c == '-') {
                pos++;
                return -factor();
            }
            if (Character.isDigit(c)) {
                return number();
            }
            throw new IllegalArgumentException("Unexpected character '" + c + "'");
        }

        private double number() {
            int start = pos;
            while (pos < input.length()
                    && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            String token = input.substring(start, pos);
            try {
                return Double.parseDouble(token);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number '" + token + "'");
            }
        }
    }
}
