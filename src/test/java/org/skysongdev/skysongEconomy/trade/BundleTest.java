package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleTest {
    private static final BundlePart.Rolls ROLLS = new BundlePart.Rolls(IntRange.exactly(2), List.of(
            new BundlePart.Rolls.Option("dagger", 3, IntRange.exactly(1)),
            new BundlePart.Rolls.Option("axe", 1, IntRange.exactly(2))));

    @Test
    void baseValueAddsFixedItemsAndAverageOfRandomParts() {
        Bundle bundle = new Bundle(List.of(
                new BundlePart.Item("sword", IntRange.exactly(2)),
                new BundlePart.Item("spear", new IntRange(1, 3)),
                ROLLS));
        MapPrices prices = MapPrices.of("sword", 10, "spear", 4, "dagger", 2, "axe", 8);
        // 2*10 + 2.0*4 + 2 rolls * (3*2*1 + 1*8*2) / 4 = 20 + 8 + 11
        assertEquals(0, new BigDecimal("39").compareTo(bundle.baseValue(prices)), bundle.baseValue(prices).toPlainString());
    }

    @Test
    void baseValueNeedsEveryItemPriced() {
        Bundle bundle = new Bundle(List.of(new BundlePart.Item("sword", IntRange.exactly(1))));
        assertThrows(PriceException.class, () -> bundle.baseValue(MapPrices.of()));
    }

    @Test
    void rollAddsUpTheSameItem() {
        Bundle bundle = new Bundle(List.of(
                new BundlePart.Item("sword", IntRange.exactly(2)),
                new BundlePart.Item("sword", IntRange.exactly(1))));
        assertEquals(List.of(new ItemAmount("sword", 3)), bundle.roll(new Random(1)));
    }

    @Test
    void rollsFollowTheWeights() {
        Random random = new Random(42);
        int daggers = 0;
        int axes = 0;
        for (int i = 0; i < 2000; i++) {
            for (ItemAmount item : ROLLS.roll(random)) {
                if (item.materialId().equals("dagger")) {
                    daggers += item.amount();
                } else {
                    axes += item.amount() / 2;
                }
            }
        }
        assertEquals(4000, daggers + axes, "two rolls each time");
        double daggerShare = daggers / 4000.0;
        assertTrue(daggerShare > 0.7 && daggerShare < 0.8, "about 3 in 4 rolls are daggers: " + daggerShare);
    }

    @Test
    void aZeroAmountGivesNothing() {
        BundlePart part = new BundlePart.Item("sword", IntRange.exactly(0));
        assertEquals(List.of(), part.roll(new Random(1)));
    }

    @Test
    void listsEveryItemItCanDeliver() {
        Bundle bundle = new Bundle(List.of(new BundlePart.Item("sword", IntRange.exactly(1)), ROLLS));
        assertEquals(Set.of("sword", "dagger", "axe"), bundle.materialIds());
    }
}
