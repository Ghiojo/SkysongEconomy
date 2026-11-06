package org.skysongdev.skysongEconomy.nodes.random;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.loot.LootGroup;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomTableParserTest {
    private final LootParser loot = new LootParser(Set.of("copper", "coal", "geode")::contains);
    private final LootGroupLibrary groups = loot.parseGroups(Map.of(
            "unskilled-ores", yaml("entries:\n  - {item: copper, weight: 3}\n  - {item: coal}")), message -> {
    });
    private final RandomTableParser parser = new RandomTableParser(loot, Set.of("mining")::contains);

    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("nodes/random/mine.yml", new StringReader(text));
    }

    private RandomTable parse(String text) {
        return parser.parse("mine", yaml(text), groups);
    }

    @Test
    void readsEachTiersCostsAndOutput() {
        RandomTable table = parse("""
                name: "<gray>Mine"
                type: Mining
                skill: Mining
                tiers:
                  1:
                    cost: 1
                    public-cost: {downtime: 1}
                    rolls:
                      - {times: 8, group: unskilled-ores}
                  3:
                    cost: 3
                    public-cost: {downtime: 1, harvest-tokens: 2, money: 5.5}
                    rolls:
                      - times: "6-8"
                        from:
                          - {group: unskilled-ores, weight: 60}
                          - {item: coal, weight: 10, amount: "1-3"}
                    chances:
                      - {item: geode, chance: 2.5}
                  4:
                    cost: 5
                    guaranteed: [{item: coal}]
                """);

        assertEquals("<gray>Mine", table.name());
        assertEquals("mining", table.type());
        assertEquals("mining", table.skill());
        assertEquals(List.of(1, 3, 4), List.copyOf(table.tiers().keySet()));
        assertEquals(Optional.of(new HarvestCost(1, 0, BigDecimal.ZERO)), table.tier(1).orElseThrow().publicCost());
        assertEquals(Optional.of(new HarvestCost(1, 2, new BigDecimal("5.5"))), table.tier(3).orElseThrow().publicCost());
        assertEquals(Optional.empty(), table.tier(4).orElseThrow().publicCost(), "no public-cost: business nodes only");
        assertEquals(5, table.tier(4).orElseThrow().cost());
        LootGroup group = table.tier(1).orElseThrow().output().rolls().get(0).group();
        assertEquals("unskilled-ores", group.id(), "named groups are shared, not copied");
        assertEquals(1, table.tier(3).orElseThrow().output().chances().size());
        assertTrue(table.tier(2).isEmpty());
    }

    @Test
    void nameAndTypeDefaultToTheFileName() {
        RandomTable table = parse("skill: mining\ntiers:\n  1: {cost: 0, guaranteed: [{item: coal}]}");

        assertEquals("mine", table.name());
        assertEquals("mine", table.type());
    }

    @Test
    void offersOnlyTheTiersUpToTheNodesTier() {
        RandomTable table = parse("""
                skill: mining
                tiers:
                  1: {cost: 1, guaranteed: [{item: coal}]}
                  2: {cost: 2, guaranteed: [{item: coal}]}
                  4: {cost: 4, guaranteed: [{item: coal}]}
                """);

        assertEquals(List.of(1, 2), table.tiersUpTo(3).stream().map(RandomTier::tier).toList());
    }

    @Test
    void refusesBrokenTables() {
        assertThrows(ConfigException.class, () -> parse("tiers:\n  1: {cost: 1, guaranteed: [{item: coal}]}"), "no skill");
        assertThrows(ConfigException.class, () -> parse("skill: cooking\ntiers:\n  1: {cost: 1, guaranteed: [{item: coal}]}"));
        assertThrows(ConfigException.class, () -> parse("skill: mining"), "no tiers");
        assertThrows(ConfigException.class, () -> parse("skill: mining\ntiers:\n  6: {cost: 1, guaranteed: [{item: coal}]}"));
        assertThrows(ConfigException.class, () -> parse("skill: mining\ntiers:\n  1: {guaranteed: [{item: coal}]}"), "no cost");
        assertThrows(ConfigException.class, () -> parse("skill: mining\ntiers:\n  1: {cost: -1, guaranteed: [{item: coal}]}"));
        assertThrows(ConfigException.class, () -> parse("skill: mining\ntiers:\n  1: {cost: 1}"), "gives nothing");
        assertThrows(ConfigException.class,
                () -> parse("skill: mining\ntiers:\n  1: {cost: 1, public-cost: {gold: 1}, guaranteed: [{item: coal}]}"));
        assertThrows(ConfigException.class,
                () -> parse("skill: mining\ntiers:\n  1: {cost: 1, public-cost: {money: -2}, guaranteed: [{item: coal}]}"));
        assertThrows(ConfigException.class,
                () -> parse("skill: mining\ntiers:\n  1: {cost: 1, rolls: [{group: missing}]}"), "unknown group");
    }
}
