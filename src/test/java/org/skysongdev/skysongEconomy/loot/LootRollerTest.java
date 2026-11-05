package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.IntRange;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootRollerTest {
    private static LootEntry.Item item(String id, int amount) {
        return new LootEntry.Item(id, IntRange.exactly(amount));
    }

    private static LootGroup only(LootEntry entry) {
        return new LootGroup("only", List.of(new WeightedEntry(entry, 1)));
    }

    private final LootRoller roller = new LootRoller(new Random(3));

    @Test
    void guaranteedItemsAreAlwaysGivenAndAddedUp() {
        LootOutput output = new LootOutput(List.of(item("wheat", 2), item("wheat", 1), item("seed", 1)), List.of(), List.of());
        assertEquals(List.of(new ItemAmount("wheat", 3), new ItemAmount("seed", 1)), roller.roll(output));
    }

    @Test
    void eachRollAddsOnePick() {
        LootOutput output = new LootOutput(List.of(), List.of(), List.of(new GroupRolls(IntRange.exactly(8), only(item("stone", 2)))));
        assertEquals(List.of(new ItemAmount("stone", 16)), roller.roll(output));
    }

    @Test
    void landingOnAGroupRollsInsideIt() {
        LootGroup gems = only(item("ruby", 1));
        LootGroup rarity = only(new LootEntry.Group(only(new LootEntry.Group(gems))));
        LootOutput output = new LootOutput(List.of(), List.of(), List.of(new GroupRolls(IntRange.exactly(3), rarity)));
        assertEquals(List.of(new ItemAmount("ruby", 3)), roller.roll(output));
    }

    @Test
    void nothingAndZeroAmountsGiveNothing() {
        LootGroup group = new LootGroup("dud", List.of(
                new WeightedEntry(new LootEntry.Nothing(), 1),
                new WeightedEntry(new LootEntry.Item("dust", new IntRange(0, 0)), 1)));
        LootOutput output = new LootOutput(List.of(), List.of(), List.of(new GroupRolls(IntRange.exactly(20), group)));
        assertEquals(List.of(), roller.roll(output));
    }

    @Test
    void chanceItemsAreRolledOnTheirOwn() {
        LootOutput certain = new LootOutput(List.of(), List.of(new ChanceItem(item("golden_seed", 1), 100)), List.of());
        assertEquals(List.of(new ItemAmount("golden_seed", 1)), roller.roll(certain));

        LootOutput quarter = new LootOutput(List.of(), List.of(new ChanceItem(item("golden_seed", 1), 25)), List.of());
        int hits = 0;
        for (int i = 0; i < 8000; i++) {
            hits += roller.roll(quarter).size();
        }
        assertTrue(hits > 1800 && hits < 2200, "hit " + hits + " times out of 8000");
    }

    @Test
    void adjusterChangesAmountsAndRolls() {
        YieldAdjuster plusOne = new YieldAdjuster() {
            @Override
            public IntRange amount(IntRange amount) {
                return new IntRange(amount.min() + 1, amount.max() + 1);
            }

            @Override
            public int rolls(int rolls) {
                return rolls + 2;
            }
        };
        LootOutput output = new LootOutput(List.of(item("wheat", 1)), List.of(),
                List.of(new GroupRolls(IntRange.exactly(1), only(item("stone", 1)))));
        // wheat 1+1; stone (1+2 rolls) x (1+1)
        assertEquals(List.of(new ItemAmount("wheat", 2), new ItemAmount("stone", 6)), roller.roll(output, plusOne));
    }

    @Test
    void negativeRollCountsMeanNoRolls() {
        YieldAdjuster minusTen = new YieldAdjuster() {
            @Override
            public IntRange amount(IntRange amount) {
                return amount;
            }

            @Override
            public int rolls(int rolls) {
                return rolls - 10;
            }
        };
        LootOutput output = new LootOutput(List.of(), List.of(), List.of(new GroupRolls(IntRange.exactly(3), only(item("stone", 1)))));
        assertEquals(List.of(), roller.roll(output, minusTen));
    }
}
