package org.skysongdev.skysongEconomy.prices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceDatabaseTest {
    private final Map<String, String> saved = new HashMap<>();
    private final Map<String, BigDecimal> savedValues = new HashMap<>();
    private PriceDatabase prices;

    @BeforeEach
    void setUp() {
        prices = new PriceDatabase(new BaseValueRepository() {
            @Override
            public Map<String, String> loadAll() {
                return new HashMap<>(saved);
            }

            @Override
            public void save(String materialId, String expression, BigDecimal valueNow) {
                saved.put(materialId, expression);
                savedValues.put(materialId, valueNow);
            }

            @Override
            public void delete(String materialId) {
                saved.remove(materialId);
            }
        });
    }

    private static String refusal(Runnable action) {
        return assertThrows(PriceException.class, action::run).messageKey();
    }

    private void assertValue(String expected, String materialId) {
        BigDecimal actual = prices.value(materialId).orElseThrow();
        assertEquals(0, new BigDecimal(expected).compareTo(actual), materialId + " is worth " + actual);
    }

    @Test
    void valuesCanFollowOtherItems() {
        prices.set("steel_ingot", "4");
        prices.set("steel_sword", "{item:steel_ingot} * 2 + 1");
        assertValue("9", "steel_sword");

        prices.set("steel_ingot", "5");
        assertValue("11", "steel_sword");
    }

    @Test
    void setReturnsAndSavesTheValueNow() {
        prices.set("steel_ingot", "4");

        BigDecimal value = prices.set("Steel_Sword", "{item:steel_ingot} * 3");

        assertEquals(0, value.compareTo(BigDecimal.valueOf(12)));
        assertEquals("{item:steel_ingot} * 3", saved.get("steel_sword"), "ids are lower case");
        assertEquals(0, savedValues.get("steel_sword").compareTo(BigDecimal.valueOf(12)));
    }

    @Test
    void refusesBaseUnknownItemsAndInvalidText() {
        assertEquals("prices.base-in-database", refusal(() -> prices.set("x", "{base} * 2")));
        assertEquals("prices.unknown-item", refusal(() -> prices.set("x", "{item:missing}")));
        assertEquals("prices.invalid-expression", refusal(() -> prices.set("x", "2 +")));
        assertTrue(prices.expression("x").isEmpty(), "nothing was saved");
    }

    @Test
    void refusesLoopsAndKeepsTheOldValue() {
        prices.set("a", "1");
        prices.set("b", "{item:a} + 1");
        prices.set("c", "{item:b} + 1");

        PriceException loop = assertThrows(PriceException.class, () -> prices.set("a", "{item:c}"));
        assertEquals("prices.loop", loop.messageKey());
        assertEquals("a -> c -> b -> a", loop.placeholders().get("path"));
        assertValue("1", "a");
        assertEquals("prices.loop", refusal(() -> prices.set("d", "{item:d}")));
    }

    @Test
    void anItemOthersUseCantBeRemoved() {
        prices.set("steel_ingot", "4");
        prices.set("steel_sword", "{item:steel_ingot} * 2");

        PriceException used = assertThrows(PriceException.class, () -> prices.remove("steel_ingot"));
        assertEquals("prices.still-used", used.messageKey());
        assertEquals("steel_sword", used.placeholders().get("users"));

        assertTrue(prices.remove("steel_sword"));
        assertTrue(prices.remove("steel_ingot"));
        assertFalse(prices.remove("steel_ingot"));
        assertTrue(saved.isEmpty());
    }

    @Test
    void contextsGiveBaseAndItems() {
        prices.set("steel_ingot", "4");
        PriceExpression price = PriceExpression.parse("{base} * 0.5 + {item:steel_ingot}");

        assertEquals(0, price.evaluate(prices.context(BigDecimal.TEN)).compareTo(BigDecimal.valueOf(9)));
        assertEquals("prices.no-base", assertThrows(PriceException.class, () -> price.evaluate(prices.context(null))).messageKey());
    }

    @Test
    void loadsOldPlainNumbersAndSkipsBrokenEntries() {
        saved.put("steel_ingot", "4.5");
        saved.put("broken", "4 +");
        List<String> warnings = new ArrayList<>();

        prices.loadAll(warnings::add);

        assertValue("4.5", "steel_ingot");
        assertEquals(Optional.empty(), prices.expression("broken"));
        assertEquals(1, warnings.size());
    }

    @Test
    void handEditedLoopsAreReportedNotFollowedForever() {
        saved.put("a", "{item:b}");
        saved.put("b", "{item:a}");
        prices.loadAll(warning -> { });

        assertEquals("prices.loop", refusal(() -> prices.value("a")));
    }
}
