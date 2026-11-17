package org.skysongdev.skysongEconomy.upkeep;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpkeepPaymentTest {
    private static final Map<String, Set<String>> TYPES = Map.of(
            "bread", Set.of("food"), "apple", Set.of("food"), "water_flask", Set.of("drink"));
    private final UpkeepPayment payment = new UpkeepPayment(id -> TYPES.getOrDefault(id, Set.of()));

    private static UpkeepCost item(String id, int amount) {
        return new UpkeepCost(new UpkeepCost.Match.Item(id), amount);
    }

    private static UpkeepCost type(String type, int amount) {
        return new UpkeepCost(new UpkeepCost.Match.GoodType(type), amount);
    }

    @Test
    void takesEachEntryFromTheFirstMatchingStacks() {
        UpkeepPayment.Outcome outcome = payment.plan(List.of(item("water_flask", 1), type("food", 3)), List.of(
                new StockStack("bread", 2), new StockStack("water_flask", 4), new StockStack("apple", 5)));

        assertEquals(new UpkeepPayment.Paid(List.of(2, 1, 1)), outcome);
    }

    @Test
    void oneItemPaysOnlyOneEntry() {
        // The bread pays the bread entry, so nothing is left for the food entry.
        UpkeepPayment.Outcome outcome = payment.plan(List.of(item("bread", 2), type("food", 1)),
                List.of(new StockStack("bread", 2)));

        assertEquals(new UpkeepPayment.Short(List.of(new UpkeepPayment.Shortfall(type("food", 1), 1))), outcome);
    }

    @Test
    void anyShortEntryMeansNothingIsTakenAndSaysWhatIsMissing() {
        UpkeepPayment.Outcome outcome = payment.plan(List.of(item("water_flask", 2), type("food", 3)),
                List.of(new StockStack("water_flask", 2), new StockStack("bread", 1)));

        assertFalse(outcome.paid());
        assertEquals(List.of(new UpkeepPayment.Shortfall(type("food", 3), 2)), ((UpkeepPayment.Short) outcome).missing());
    }

    @Test
    void itemsWithoutAMaterialIdPayNothing() {
        assertFalse(payment.plan(List.of(type("food", 1)), List.of(new StockStack(null, 64))).paid());
    }

    @Test
    void countsHowManyCyclesInARowTheStockPays() {
        List<UpkeepCost> cost = List.of(item("water_flask", 1), type("food", 2));
        List<StockStack> stock = List.of(new StockStack("water_flask", 5), new StockStack("bread", 3),
                new StockStack("apple", 4));

        assertEquals(3, payment.cyclesCovered(cost, stock, 10));
        assertEquals(2, payment.cyclesCovered(cost, stock, 2));
        assertEquals(0, payment.cyclesCovered(cost, List.of(), 10));
    }

    @Test
    void acceptsOnlyItemsThatPayAnEntry() {
        List<UpkeepCost> cost = List.of(item("water_flask", 1), type("food", 2));

        assertTrue(payment.accepts(cost, "water_flask"));
        assertTrue(payment.accepts(cost, "apple"));
        assertFalse(payment.accepts(cost, "iron_ingot"));
    }
}
