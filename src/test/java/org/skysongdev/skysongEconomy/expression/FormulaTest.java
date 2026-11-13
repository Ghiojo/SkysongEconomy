package org.skysongdev.skysongEconomy.expression;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaTest {
    /** Accepts {a} and {b}, and writes them in upper case. */
    private static final Placeholders LETTERS = written -> {
        if (written.equals("a") || written.equals("b")) {
            return written.toUpperCase();
        }
        throw new Placeholders.Unknown("has {" + written + "}: use {a} or {b}");
    };
    private static final Map<String, BigDecimal> VALUES = Map.of("A", new BigDecimal("10"), "B", new BigDecimal("4"));

    private static BigDecimal eval(String text) {
        return Formula.parse(text, LETTERS).evaluate(VALUES::get);
    }

    private static void assertValue(String expected, String formula) {
        BigDecimal actual = eval(formula);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), formula + " gave " + actual);
    }

    private static String problem(String text) {
        FormulaException error = assertThrows(FormulaException.class, () -> Formula.parse(text, LETTERS));
        assertEquals(FormulaException.Kind.SYNTAX, error.kind());
        assertEquals(text.strip(), error.formula());
        return error.problem();
    }

    @Test
    void numbersAndOperatorsFollowTheUsualPrecedence() {
        assertValue("12.5", "12.5");
        assertValue("7", "1 + 2 * 3");
        assertValue("9", "(1 + 2) * 3");
        assertValue("2.5", "10 / 4");
        assertValue("1", "5 - 2 - 2");
        assertValue("-3", "-(1 + 2)");
        assertValue("5", "+5");
        assertValue("0.5", ".5");
    }

    @Test
    void placeholdersGetTheirValuesByCanonicalName() {
        assertValue("13", "3 + {a}");
        assertValue("36", "({a} - 1) * { b }");
    }

    @Test
    void listsThePlaceholdersItUsesOnceEachInOrder() {
        Formula formula = Formula.parse("{b} + {a} * {b}", LETTERS);

        assertEquals(List.of("B", "A"), List.copyOf(formula.placeholders()));
        assertTrue(Formula.parse("3", LETTERS).placeholders().isEmpty());
    }

    @Test
    void keepsTheTextAsWritten() {
        assertEquals("{a} * 2", Formula.parse("  {a} * 2 ", LETTERS).text());
        assertEquals(Formula.parse("1+1", LETTERS), Formula.parse(" 1+1", LETTERS));
        assertEquals("1+1", Formula.parse("1+1", LETTERS).toString());
    }

    @Test
    void explainsInvalidFormulas() {
        assertEquals("is empty", problem("  "));
        assertEquals("is missing a ')'", problem("(1 + 2"));
        assertEquals("ends too early", problem("1 +"));
        assertEquals("has '*' where a value was expected", problem("* 2"));
        assertEquals("has 'x' where an operator was expected", problem("2 x 3"));
        assertEquals("has a '{' without '}'", problem("{a"));
        assertEquals("has {c}: use {a} or {b}", problem("{c}"));
        assertEquals("has '1.2.3', which isn't a number", problem("1.2.3"));
    }

    @Test
    void refusesDivisionByZeroWhenWorkedOut() {
        Formula formula = Formula.parse("1 / ({a} - 10)", LETTERS);

        FormulaException error = assertThrows(FormulaException.class, () -> formula.evaluate(VALUES::get));

        assertEquals(FormulaException.Kind.DIVISION_BY_ZERO, error.kind());
        assertEquals("divides by zero", error.problem());
    }

    @Test
    void passesOnWhatAValueLookupThrows() {
        IllegalStateException refused = new IllegalStateException("no value");
        Formula formula = Formula.parse("{a}", LETTERS);

        assertSame(refused, assertThrows(IllegalStateException.class, () -> formula.evaluate(name -> {
            throw refused;
        })));
    }

    @Test
    void divisionKeepsEnoughPrecision() {
        BigDecimal error = eval("1 / 3 * 3").subtract(BigDecimal.ONE).abs();
        assertTrue(error.compareTo(new BigDecimal("0.000000000001")) < 0, "off by " + error);
    }
}
