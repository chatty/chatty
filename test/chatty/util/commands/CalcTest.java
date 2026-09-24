package chatty.util.commands;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CalcTest {

    @Test
    public void rejectsMissingClosingParenthesis() {
        assertEquals("Missing closing parenthesis", failureMessage("(1 + 2"));
    }

    @Test
    public void evaluatesBalancedParentheses() {
        assertEquals(3.0, Calc.eval("(1 + 2)"), 0.0);
    }

    private static String failureMessage(String expression) {
        try {
            Calc.eval(expression);
            throw new AssertionError("Expected expression to fail");
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}
