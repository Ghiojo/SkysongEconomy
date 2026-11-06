package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LootOddsTest {
    private static final double EXACT = 1e-9;

    private static LootEntry.Item item(String id, int min, int max) {
        return new LootEntry.Item(id, new IntRange(min, max));
    }

    private static ItemOdds odds(List<ItemOdds> all, String id) {
        return all.stream().filter(odds -> odds.materialId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void guaranteedItemsAlwaysComeAndChanceItemsComeAtTheirPercent() {
        LootOutput output = new LootOutput(List.of(item("wheat", 2, 4)),
                List.of(new ChanceItem(item("golden_seed", 1, 3), 25)), List.of());

        List<ItemOdds> all = LootOdds.of(output);

        assertEquals(new ItemOdds("wheat", 1, 3), all.get(0), "most likely first");
        assertEquals(0.25, odds(all, "golden_seed").chance(), EXACT);
        assertEquals(0.5, odds(all, "golden_seed").averageAmount(), EXACT, "a quarter of 2 on average");
    }

    @Test
    void rollsMultiplyTheChanceThroughNestedGroups() {
        LootGroup gems = new LootGroup("gems", List.of(
                new WeightedEntry(item("ruby", 1, 1), 1),
                new WeightedEntry(item("opal", 1, 1), 3)));
        LootGroup rarity = new LootGroup("rarity", List.of(
                new WeightedEntry(new LootEntry.Group(gems), 1),
                new WeightedEntry(item("coal", 2, 4), 2),
                new WeightedEntry(new LootEntry.Nothing(), 1)));
        LootOutput output = new LootOutput(List.of(), List.of(), List.of(new GroupRolls(IntRange.exactly(2), rarity)));

        List<ItemOdds> all = LootOdds.of(output);

        // One roll: coal 2/4, gems 1/4 of which ruby 1/4, so ruby 1/16.
        assertEquals(1 - Math.pow(0.5, 2), odds(all, "coal").chance(), EXACT);
        assertEquals(2 * 0.5 * 3, odds(all, "coal").averageAmount(), EXACT);
        assertEquals(1 - Math.pow(15.0 / 16, 2), odds(all, "ruby").chance(), EXACT);
        assertEquals(2.0 / 16, odds(all, "ruby").averageAmount(), EXACT);
        assertEquals(List.of("coal", "opal", "ruby"), all.stream().map(ItemOdds::materialId).toList());
    }

    @Test
    void aRollRangeAveragesTheChanceOverEachCount() {
        LootGroup half = new LootGroup("half", List.of(
                new WeightedEntry(item("tin", 1, 1), 1),
                new WeightedEntry(new LootEntry.Nothing(), 1)));
        LootOutput output = new LootOutput(List.of(), List.of(), List.of(new GroupRolls(new IntRange(1, 3), half)));

        ItemOdds tin = LootOdds.of(output).get(0);

        assertEquals(1 - (0.5 + 0.25 + 0.125) / 3, tin.chance(), EXACT);
        assertEquals(2 * 0.5, tin.averageAmount(), EXACT, "2 rolls on average, half of them tin");
    }

    @Test
    void anItemFromSeveralSourcesAddsUp() {
        LootGroup coal = new LootGroup("coal", List.of(new WeightedEntry(item("coal", 1, 1), 1),
                new WeightedEntry(new LootEntry.Nothing(), 1)));
        LootOutput output = new LootOutput(List.of(), List.of(new ChanceItem(item("coal", 1, 1), 50)),
                List.of(new GroupRolls(IntRange.exactly(1), coal)));

        ItemOdds odds = LootOdds.of(output).get(0);

        assertEquals(0.75, odds.chance(), EXACT, "missing both is a half times a half");
        assertEquals(1, odds.averageAmount(), EXACT);
    }

    @Test
    void theAverageValueLeavesOutUnpricedItems() {
        List<ItemOdds> odds = List.of(new ItemOdds("coal", 1, 3), new ItemOdds("ruby", 0.1, 0.5), new ItemOdds("dust", 1, 9));
        Map<String, BigDecimal> prices = Map.of("coal", new BigDecimal("2"), "ruby", new BigDecimal("40"));

        LootOdds.AverageValue value = LootOdds.averageValue(odds, id -> Optional.ofNullable(prices.get(id)));

        assertEquals(0, new BigDecimal("26").compareTo(value.value()), value.value().toPlainString());
        assertEquals(List.of("dust"), value.unpriced());
    }
}
