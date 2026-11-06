package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootGroupTest {
    private static final LootEntry COPPER = new LootEntry.Item("copper", IntRange.exactly(1));
    private static final LootEntry IRON = new LootEntry.Item("iron", IntRange.exactly(1));

    @Test
    void weightsNeedNotAddUpToAHundred() {
        LootGroup group = new LootGroup("ores", List.of(new WeightedEntry(COPPER, 1), new WeightedEntry(IRON, 4)));
        assertEquals(5, group.totalWeight());
        assertEquals(0.2, group.chance(group.entries().get(0)), 1e-9);
        assertEquals(0.8, group.chance(group.entries().get(1)), 1e-9);
    }

    @Test
    void picksFollowTheWeights() {
        LootGroup group = new LootGroup("ores", List.of(new WeightedEntry(COPPER, 1), new WeightedEntry(IRON, 4)));
        Random random = new Random(7);
        int copper = 0;
        int rolls = 10_000;
        for (int i = 0; i < rolls; i++) {
            if (group.pick(random).equals(COPPER)) {
                copper++;
            }
        }
        // 1 in 5 is 2000; allow for chance.
        assertTrue(copper > 1800 && copper < 2200, "copper was picked " + copper + " times");
    }

    @Test
    void everyTicketLandsOnTheEntryItFallsIn() {
        LootGroup group = new LootGroup("ores", List.of(new WeightedEntry(COPPER, 1), new WeightedEntry(IRON, 4)));
        assertEquals(COPPER, group.pick(new FixedTicket(0)));
        assertEquals(IRON, group.pick(new FixedTicket(1)));
        assertEquals(IRON, group.pick(new FixedTicket(4)));
    }

    @Test
    void refusesEmptyGroupsAndWeightsBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new LootGroup("empty", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WeightedEntry(COPPER, 0));
    }

    @Test
    void onlyGroupFilesAreNamedNotListsWrittenInline() {
        WeightedEntry coal = new WeightedEntry(new LootEntry.Item("coal", IntRange.exactly(1)), 1);

        assertTrue(new LootGroup("unskilled-ores", List.of(coal)).isNamed());
        assertFalse(new LootGroup("nodes/random/mine.yml: tiers.1.rolls[0]", List.of(coal)).isNamed());
    }

    /** Always rolls the same ticket. */
    private record FixedTicket(int ticket) implements java.util.random.RandomGenerator {
        @Override
        public long nextLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextInt(int bound) {
            return ticket;
        }
    }
}
