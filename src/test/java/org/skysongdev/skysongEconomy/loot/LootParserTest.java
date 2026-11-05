package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootParserTest {
    private static final Set<String> ITEMS = Set.of("copper", "tin", "coal", "ruby", "wheat", "golden_seed");

    private final LootParser parser = new LootParser(ITEMS::contains);
    private final List<String> warnings = new ArrayList<>();

    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("nodes/test.yml", new StringReader(text));
    }

    /** @param files alternating group id and file text */
    private LootGroupLibrary groups(String... files) {
        Map<String, ConfigSection> sections = new LinkedHashMap<>();
        for (int i = 0; i < files.length; i += 2) {
            sections.put(files[i], YamlFiles.parse("nodes/groups/" + files[i] + ".yml", new StringReader(files[i + 1])));
        }
        return parser.parseGroups(sections, warnings::add);
    }

    @Test
    void readsEveryKindOfEntry() {
        LootGroupLibrary library = groups(
                "unskilled-ores", """
                        entries:
                          - {item: copper, weight: 10, amount: "1-3"}
                          - {group: gems, weight: 1}
                          - weight: 2
                            from:
                              - {item: tin}
                              - {item: coal, weight: 3, amount: 2}
                          - {nothing: true}
                        """,
                "gems", "entries: [{item: ruby}]\n");
        assertEquals(List.of(), warnings);
        LootGroup ores = library.find("unskilled-ores").orElseThrow();
        assertEquals(14, ores.totalWeight());
        assertEquals(new LootEntry.Item("copper", new IntRange(1, 3)), ores.entries().get(0).entry());
        assertSame(library.find("gems").orElseThrow(), ((LootEntry.Group) ores.entries().get(1).entry()).group());
        assertEquals(4, ((LootEntry.Group) ores.entries().get(2).entry()).group().totalWeight());
        assertInstanceOf(LootEntry.Nothing.class, ores.entries().get(3).entry());
        assertEquals(1, ores.entries().get(3).weight());
    }

    @Test
    void groupsMayPointToFilesLoadedLater() {
        LootGroupLibrary library = groups("a", "entries: [{group: z}]\n", "z", "entries: [{item: coal}]\n");
        assertSame(library.find("z").orElseThrow(),
                ((LootEntry.Group) library.find("a").orElseThrow().entries().get(0).entry()).group());
    }

    @Test
    void loopsAreSkippedWithAWarning() {
        LootGroupLibrary library = groups(
                "a", "entries: [{group: b}]\n",
                "b", "entries: [{item: coal}, {from: [{group: a}]}]\n",
                "fine", "entries: [{item: coal}]\n");
        assertEquals(Set.of("fine"), library.ids());
        assertTrue(warnings.get(0).contains("a -> b -> a"), warnings.get(0));
        groups("self", "entries: [{group: self}]\n");
        assertTrue(warnings.get(warnings.size() - 1).contains("self -> self"));
    }

    @Test
    void aBrokenGroupSkipsOnlyItselfAndTheGroupsUsingIt() {
        LootGroupLibrary library = groups(
                "broken", "entries: [{item: diamond}]\n",
                "uses-broken", "entries: [{group: broken}]\n",
                "fine", "entries: [{item: coal}]\n");
        assertEquals(Set.of("fine"), library.ids());
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("'broken'") && warnings.get(0).contains("diamond"), warnings.get(0));
    }

    @Test
    void refusesBrokenEntries() {
        String[] broken = {
                "entries: [{group: missing}]\n",            // unknown group
                "entries: [{item: coal, weight: 0}]\n",     // weight 0
                "entries: [{item: coal, weight: 1.5}]\n",   // weight not whole
                "entries: [{item: coal, nothing: true}]\n", // two kinds
                "entries: [{weight: 2}]\n",                 // no kind
                "entries: []\n",                            // empty
                "other: 1\n",                               // no entries
                "entries: [{item: coal, amount: \"3-1\"}]\n" // reversed range
        };
        for (String file : broken) {
            assertEquals(Set.of(), groups("g", file).ids(), file);
        }
        assertEquals(broken.length, warnings.size());
    }

    @Test
    void readsAnOutput() {
        LootGroupLibrary library = groups("unskilled-ores", "entries: [{item: copper}]\n");
        LootOutput output = parser.parseOutput(yaml("""
                guaranteed:
                  - {item: wheat, amount: "2-4"}
                chances:
                  - {item: golden_seed, chance: 2.5, amount: "1-2"}
                rolls:
                  - times: "6-8"
                    from:
                      - {group: unskilled-ores, weight: 60}
                      - {item: wheat, weight: 10, amount: "1-3"}
                      - {nothing: true, weight: 5}
                  - {group: unskilled-ores}
                """), library);
        assertEquals(List.of(new LootEntry.Item("wheat", new IntRange(2, 4))), output.guaranteed());
        assertEquals(List.of(new ChanceItem(new LootEntry.Item("golden_seed", new IntRange(1, 2)), 2.5)), output.chances());
        GroupRolls first = output.rolls().get(0);
        assertEquals(new IntRange(6, 8), first.count());
        assertEquals(75, first.group().totalWeight());
        assertSame(library.find("unskilled-ores").orElseThrow(), ((LootEntry.Group) first.group().entries().get(0).entry()).group());
        assertEquals(new LootEntry.Item("wheat", new IntRange(1, 3)), first.group().entries().get(1).entry());
        assertEquals(IntRange.exactly(1), output.rolls().get(1).count());
        assertSame(library.find("unskilled-ores").orElseThrow(), output.rolls().get(1).group());
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
        assertThrows(ConfigException.class, () -> parser.parseOutput(yaml("guaranteed: [{item: diamond}]\n"), none), "unknown item");
    }
}
