package org.skysongdev.skysongEconomy.nodes.farm;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.trade.IntRange;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmTypeParserTest {
    private final List<String> warnings = new ArrayList<>();
    private final FarmTypeParser parser = new FarmTypeParser(Set.of("animalhandling")::contains, warnings::add);
    private final FileRegistry<FarmResource> resources = new FileRegistry<>(List.of(
            resource("sheep", "sheep_token"), resource("cow", "cow_token"), resource("lamb", "sheep_token")));

    private static FarmResource resource(String id, String primer) {
        LootOutput wool = new LootOutput(List.of(new LootEntry.Item("wool", IntRange.exactly(1))), List.of(), List.of());
        return new FarmResource(id, id, new ItemAmount(primer, 1), Map.of(1, new FarmTier(1, 1, wool)));
    }

    private FarmType parse(String text) {
        ConfigSection root = YamlFiles.parse("nodes/farm/types/animal.yml", new StringReader(text));
        return parser.parse("animal", root, resources);
    }

    @Test
    void readsTheSkillAndResources() {
        FarmType animal = parse("name: \"<gold>Animal pen\"\nskill: AnimalHandling\nresources: [Sheep, cow]");

        assertEquals("<gold>Animal pen", animal.name());
        assertEquals("animalhandling", animal.skill());
        assertEquals(List.of("sheep", "cow"), animal.resources());
        assertTrue(animal.accepts("cow"));
        assertFalse(animal.accepts("lamb"));
        assertEquals(List.of(), warnings);
    }

    @Test
    void theNameDefaultsToTheFile() {
        assertEquals("animal", parse("skill: animalhandling\nresources: [cow]").name());
    }

    @Test
    void aResourceThatDidNotLoadIsLeftOutWithAWarning() {
        FarmType animal = parse("skill: animalhandling\nresources: [sheep, goat, cow, sheep]");

        assertEquals(List.of("sheep", "cow"), animal.resources(), "goat is left out, and sheep is listed once");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("goat"), warnings.get(0));
    }

    @Test
    void warnsWhenTwoResourcesShareAPrimerItem() {
        FarmType animal = parse("skill: animalhandling\nresources: [sheep, lamb]");

        assertEquals(List.of("sheep", "lamb"), animal.resources(), "both still load");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("sheep_token") && warnings.get(0).contains("plants 'sheep'"), warnings.get(0));
    }

    @Test
    void refusesAMissingOrUnknownSkillAndAnEmptyList() {
        assertThrows(ConfigException.class, () -> parse("resources: [cow]"));
        assertTrue(assertThrows(ConfigException.class, () -> parse("skill: mining\nresources: [cow]"))
                .getMessage().contains("mining"));
        assertTrue(assertThrows(ConfigException.class, () -> parse("skill: animalhandling"))
                .getMessage().contains("resources"));
    }
}
