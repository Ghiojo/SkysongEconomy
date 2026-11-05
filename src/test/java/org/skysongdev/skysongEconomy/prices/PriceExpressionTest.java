package org.skysongdev.skysongEconomy.prices;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceExpressionTest {
    /** {base} is 10; steel_ingot is 4 and leather is 1.5. */
    private static final PriceContext CONTEXT = new PriceContext() {
        private final Map<String, BigDecimal> items = Map.of("steel_ingot", new BigDecimal("4"), "leather", new BigDecimal("1.5"));

        @Override
        public BigDecimal base() {
            return BigDecimal.TEN;
        }

        @Override
        public BigDecimal item(String materialId) {
            BigDecimal value = items.get(materialId);
            if (value == null) {
                throw new PriceException("prices.unknown-item", Map.of("material", materialId));
            }
            return value;
        }
    };

    private static BigDecimal eval(String text) {
        return PriceExpression.parse(text).evaluate(CONTEXT);
    }

    private static void assertValue(String expected, String expression) {
        BigDecimal actual = eval(expression);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), expression + " gave " + actual);
    }

    private static String problem(String text) {
        PriceException error = assertThrows(PriceException.class, () -> PriceExpression.parse(text));
        assertEquals("prices.invalid-expression", error.messageKey());
        return error.placeholders().get("problem");
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
    void placeholdersUseTheContext() {
        assertValue("13", "{base} * 1.3");
        assertValue("9", "{item:steel_ingot} * 2 + 1");
        assertValue("5.5", "{ item: steel_ingot } + {item:leather}");
    }

    @Test
    void reportsWhatItUses() {
        PriceExpression expression = PriceExpression.parse("{base} + {item:steel_ingot} * {item:leather} - {item:steel_ingot}");

        assertTrue(expression.usesBase());
        assertEquals(List.of("steel_ingot", "leather"), List.copyOf(expression.items()));
        assertFalse(PriceExpression.parse("3").usesBase());
    }

    @Test
    void keepsTheTextAsWritten() {
        assertEquals("{base} * 2", PriceExpression.parse("  {base} * 2 ").text());
        assertEquals(PriceExpression.parse("1+1"), PriceExpression.parse(" 1+1"));
    }

    @Test
    void divisionKeepsEnoughPrecision() {
        BigDecimal error = eval("1 / 3 * 3").subtract(BigDecimal.ONE).abs();
        assertTrue(error.compareTo(new BigDecimal("0.000000000001")) < 0, "off by " + error);
    }

    @Test
    void explainsInvalidExpressions() {
        assertEquals("is empty", problem("  "));
        assertEquals("is missing a ')'", problem("(1 + 2"));
        assertEquals("ends too early", problem("1 +"));
        assertEquals("has '*' where a value was expected", problem("* 2"));
        assertEquals("has 'x' where an operator was expected", problem("2 x 3"));
        assertEquals("has a '{' without '}'", problem("{base"));
        assertEquals("has {cost}: use {base} or {item:<material_id>}", problem("{cost}"));
        assertEquals("has {item:} without a material id", problem("{item:}"));
        assertEquals("has '1.2.3', which isn't a number", problem("1.2.3"));
    }

    @Test
    void refusesDivisionByZeroAndMissingItems() {
        assertEquals("prices.division-by-zero", assertThrows(PriceException.class, () -> eval("1 / (2 - 2)")).messageKey());
        assertEquals("prices.unknown-item", assertThrows(PriceException.class, () -> eval("{item:gold}")).messageKey());
    }

    @Test
    void ofMakesAPlainNumber() {
        assertValue("4.25", PriceExpression.of(new BigDecimal("4.25")).text());
    }
}
