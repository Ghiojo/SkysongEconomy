package org.skysongdev.skysongEconomy.nodes.farm;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmResourceParserTest {
    private static final Set<String> ITEMS = Set.of("sheep_token", "wool", "mutton", "fleece");
    private final LootParser loot = new LootParser(ITEMS::contains);
    private final LootGroupLibrary groups = loot.parseGroups(Map.of(
            "rare-animal-drops", yaml("entries:\n  - {item: fleece}")), message -> {
    });
    private final FarmResourceParser parser = new FarmResourceParser(loot, ITEMS::contains);

    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("nodes/farm/resources/sheep.yml", new StringReader(text));
    }

    private FarmResource parse(String text) {
        return parser.parse("sheep", yaml(text), groups);
    }

    @Test
    void readsThePrimerAndOnlyTheTiersListed() {
        FarmResource sheep = parse("""
                name: "<white>Sheep"
                primer: {item: Sheep_Token, amount: 2}
                tiers:
                  3:
                    cost: 3
                    guaranteed: [{item: wool, amount: "2-3"}]
                    chances: [{item: mutton, chance: 25, amount: "1-2"}]
                  4:
                    cost: 4
                    rolls: [{times: 1, group: rare-animal-drops}]
                """);

        assertEquals("sheep", sheep.id());
        assertEquals("<white>Sheep", sheep.name());
        assertEquals(new ItemAmount("sheep_token", 2), sheep.primer());
        assertEquals(Set.of(3, 4), sheep.tiers().keySet());
        assertEquals(3, sheep.tier(3).orElseThrow().cost());
        assertTrue(sheep.tier(1).isEmpty());
        assertEquals(List.of(), sheep.tiersUpTo(2), "a tier 2 node offers nothing of a resource that starts at 3");
        assertEquals(List.of(3), sheep.tiersUpTo(3).stream().map(FarmTier::tier).toList());
        assertEquals(List.of(3, 4), sheep.tiersUpTo(5).stream().map(FarmTier::tier).toList());
    }

    @Test
    void theNameDefaultsToTheFileAndThePrimerAmountToOne() {
        FarmResource sheep = parse("primer: {item: sheep_token}\ntiers:\n  1: {cost: 0, guaranteed: [{item: wool}]}");

        assertEquals("sheep", sheep.name());
        assertEquals(new ItemAmount("sheep_token", 1), sheep.primer());
        assertEquals(0, sheep.tier(1).orElseThrow().cost(), "a free tier is allowed");
    }

    @Test
    void refusesAMissingOrUnknownPrimer() {
        String tiers = "tiers:\n  1: {cost: 1, guaranteed: [{item: wool}]}";

        assertTrue(assertThrows(ConfigException.class, () -> parse(tiers)).getMessage().contains("primer"));
        assertTrue(assertThrows(ConfigException.class, () -> parse("primer: {item: goat_token}\n" + tiers))
                .getMessage().contains("goat_token"));
        assertTrue(assertThrows(ConfigException.class, () -> parse("primer: {item: sheep_token, amount: 0}\n" + tiers))
                .getMessage().contains("amount"));
    }

    @Test
    void refusesBrokenTiers() {
        String primer = "primer: {item: sheep_token}\n";

        assertThrows(ConfigException.class, () -> parse(primer + "tiers: {}"), "at least one tier");
        assertThrows(ConfigException.class, () -> parse(primer + "tiers:\n  6: {cost: 1, guaranteed: [{item: wool}]}"));
        assertTrue(assertThrows(ConfigException.class, () -> parse(primer + "tiers:\n  1: {guaranteed: [{item: wool}]}"))
                .getMessage().contains("cost"));
        assertThrows(ConfigException.class, () -> parse(primer + "tiers:\n  1: {cost: -1, guaranteed: [{item: wool}]}"));
        assertThrows(ConfigException.class, () -> parse(primer + "tiers:\n  1: {cost: 1}"), "a tier must give something");
        assertThrows(ConfigException.class, () -> parse(primer + "tiers:\n  1: {cost: 1, rolls: [{group: gems}]}"));
    }
}
