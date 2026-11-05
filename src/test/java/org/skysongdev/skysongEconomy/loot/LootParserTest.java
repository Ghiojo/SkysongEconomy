package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.io.StringReader;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootParserTest {
    private static final Set<String> ITEMS = Set.of("copper_ore", "tin_ore", "coal", "ruby", "wheat", "golden_seed");

    private final LootParser parser = new LootParser(ITEMS::contains);

    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("nodes/test.yml", new StringReader(text));
    }

    private LootGroupLibrary groups(String text) {
        return parser.parseGroups(yaml(text).section("groups"), LootGroupLibrary.empty());
    }

    @Test
    void readsEveryKindOfEntry() {
        LootGroupLibrary library = groups("""
                groups:
                  common:
                    - {item: copper_ore, weight: 6, amount: "1-3"}
                    - {group: gems, weight: 1}
                    - weight: 2
                      from:
                        - {item: tin_ore}
                        - {item: coal, weight: 3, amount: 2}
                    - {nothing: true}
                  gems:
                    - {item: ruby}
                """);
        LootGroup common = library.find("common").orElseThrow();
        assertEquals(10, common.totalWeight());
        assertEquals(new LootEntry.Item("copper_ore", new IntRange(1, 3)), common.entries().get(0).entry());
        assertSame(library.find("gems").orElseThrow(), ((LootEntry.Group) common.entries().get(1).entry()).group());
        LootGroup inline = ((LootEntry.Group) common.entries().get(2).entry()).group();
        assertEquals(4, inline.totalWeight());
        assertInstanceOf(LootEntry.Nothing.class, common.entries().get(3).entry());
        assertEquals(1, common.entries().get(3).weight());
    }

    @Test
    void groupsMayPointToSharedGroupsAndShadowThem() {
        LootGroupLibrary shared = groups("""
                groups:
                  gems: [{item: ruby}]
                  ores: [{item: coal}]
                """);
        LootGroupLibrary own = parser.parseGroups(yaml("""
                groups:
                  ores: [{item: copper_ore}]
                  common: [{group: gems}, {group: ores}]
                """).section("groups"), shared);
        LootGroup common = own.find("common").orElseThrow();
        assertSame(shared.find("gems").orElseThrow(), ((LootEntry.Group) common.entries().get(0).entry()).group());
        assertSame(own.find("ores").orElseThrow(), ((LootEntry.Group) common.entries().get(1).entry()).group());
        assertEquals(Set.of("gems", "ores", "common"), own.with(shared).ids());
        assertSame(own.find("ores").orElseThrow(), own.with(shared).find("ores").orElseThrow());
    }

    @Test
    void refusesLoops() {
        ConfigException loop = assertThrows(ConfigException.class, () -> groups("""
                groups:
                  a: [{group: b}]
                  b: [{item: coal}, {from: [{group: a}]}]
                """));
        assertTrue(loop.getMessage().contains("a -> b -> a"), loop.getMessage());
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{group: a}]\n"));
    }

    @Test
    void refusesBrokenEntries() {
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{item: diamond}]\n"), "unknown item");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{group: missing}]\n"), "unknown group");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{item: coal, weight: 0}]\n"), "weight 0");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{item: coal, weight: 1.5}]\n"), "weight 1.5");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{item: coal, group: b}]\n  b: [{item: coal}]\n"), "two kinds");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{weight: 2}]\n"), "no kind");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: []\n"), "empty");
        assertThrows(ConfigException.class, () -> groups("groups:\n  Big Group: [{item: coal}]\n"), "bad name");
        assertThrows(ConfigException.class, () -> groups("groups:\n  a: [{item: coal, amount: \"3-1\"}]\n"), "reversed range");
    }

    @Test
    void readsAnOutput() {
        LootGroupLibrary library = groups("groups:\n  common: [{item: copper_ore}]\n");
        LootOutput output = parser.parseOutput(yaml("""
                guaranteed:
                  - {item: wheat, amount: "2-4"}
                chances:
                  - {item: golden_seed, chance: 2.5}
                rolls:
                  - {times: "6-8", group: common}
                  - {from: [{item: coal}]}
                """), library);
        assertEquals(List.of(new LootEntry.Item("wheat", new IntRange(2, 4))), output.guaranteed());
        assertEquals(List.of(new ChanceItem(new LootEntry.Item("golden_seed", IntRange.exactly(1)), 2.5)), output.chances());
        assertEquals(new IntRange(6, 8), output.rolls().get(0).count());
        assertSame(library.find("common").orElseThrow(), output.rolls().get(0).group());
        assertEquals(IntRange.exactly(1), output.rolls().get(1).count());
    }

    @Test
    void refusesBrokenOutputs() {
        LootGroupLibrary none = LootGroupLibrary.empty();
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("other: 1\n"), none), "gives nothing");
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("chances: [{item: coal}]\n"), none), "no chance");
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("chances: [{item: coal, chance: 0}]\n"), none), "chance 0");
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("chances: [{item: coal, chance: 101}]\n"), none), "chance 101");
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("rolls: [{times: 2}]\n"), none), "no target");
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("rolls: [{group: missing}]\n"), none), "unknown group");
    }
}
