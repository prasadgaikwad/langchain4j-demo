package dev.prasadgaikwad.langchain4jdemo.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorToolTest {

    private final CalculatorTool calculatorTool = new CalculatorTool();

    @Test
    void evaluatesBasicArithmetic() {
        assertThat(calculatorTool.calculate("1 + 2")).isEqualTo(3.0);
        assertThat(calculatorTool.calculate("7 - 10")).isEqualTo(-3.0);
        assertThat(calculatorTool.calculate("6 * 7")).isEqualTo(42.0);
        assertThat(calculatorTool.calculate("10 / 4")).isEqualTo(2.5);
    }

    @Test
    void respectsOperatorPrecedence() {
        assertThat(calculatorTool.calculate("2 + 3 * 4")).isEqualTo(14.0);
        assertThat(calculatorTool.calculate("20 - 4 / 2")).isEqualTo(18.0);
    }

    @Test
    void handlesParenthesesAndNegation() {
        assertThat(calculatorTool.calculate("(2 + 3) * 4")).isEqualTo(20.0);
        assertThat(calculatorTool.calculate("-(5 + 1)")).isEqualTo(-6.0);
    }

    @Test
    void handlesDecimals() {
        assertThat(calculatorTool.calculate("1.5 * 2")).isEqualTo(3.0);
    }

    @Test
    void rejectsInvalidExpressions() {
        assertThatThrownBy(() -> calculatorTool.calculate("1 +"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculatorTool.calculate("(1 + 2"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculatorTool.calculate("10 / 0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculatorTool.calculate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isArithmeticDetectsNumericExpressionsOnly() {
        assertThat(CalculatorTool.isArithmetic("2 + 3 * 4")).isTrue();
        assertThat(CalculatorTool.isArithmetic("(10 - 2) / 4")).isTrue();
        assertThat(CalculatorTool.isArithmetic("what is 2 plus 2")).isFalse();
        assertThat(CalculatorTool.isArithmetic("hello")).isFalse();
        assertThat(CalculatorTool.isArithmetic("42")).isFalse();
    }
}
