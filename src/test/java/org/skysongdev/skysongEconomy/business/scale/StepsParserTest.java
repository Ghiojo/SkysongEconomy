package org.skysongdev.skysongEconomy.business.scale;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StepsParserTest {
    private static Steps<String> tiers(String yaml) {
        ConfigSection root = YamlFiles.parse("npcs.yml", new StringReader(yaml));
        return StepsParser.parse(root.section("max-tier"), ConfigSection::requireString);
    }

    private static BusinessScale scale(int level, int tier) {
        return new BusinessScale(level, tier);
    }

    private static String error(String yaml) {
        return assertThrows(ConfigException.class, () -> tiers(yaml)).getMessage();
    }

    @Test
    void theHighestRowReachedApplies() {
        Steps<String> steps = tiers("max-tier: {by: level, 1: proficient, 4: adept}");

        assertEquals(Basis.LEVEL, steps.basis());
        assertEquals(Optional.empty(), steps.at(scale(0, 5)));
        assertEquals(Optional.of("proficient"), steps.at(scale(1, 5)));
        assertEquals(Optional.of("proficient"), steps.at(scale(3, 5)));
        assertEquals(Optional.of("adept"), steps.at(scale(4, 4)));
        assertEquals(Optional.of("adept"), steps.at(scale(5, 5)));
    }

    @Test
    void byTierLooksAtTheBusinessTier() {
        Steps<String> steps = tiers("max-tier: {by: Tier, 4: adept, 1: proficient}");

        assertEquals(Basis.TIER, steps.basis());
        assertEquals(Optional.of("proficient"), steps.at(scale(5, 3)));
        assertEquals(Optional.of("adept"), steps.at(scale(0, 4)));
        assertEquals(List.of(1, 4), List.copyOf(steps.rows().keySet()), "rows are kept lowest first");
    }

    @Test
    void reachedListsEveryRowUpToTheScale() {
        Steps<String> steps = tiers("max-tier: {1: a, 3: b, 5: c}");

        assertEquals(List.of(), steps.reached(scale(0, 1)));
        assertEquals(List.of("a"), steps.reached(scale(2, 1)));
        assertEquals(List.of("a", "b"), steps.reached(scale(3, 1)));
        assertEquals(List.of("a", "b", "c"), steps.reached(scale(5, 1)));
    }

    @Test
    void explainsWhatIsWrong() {
        assertEquals("npcs.yml: max-tier.by must be level or tier, not 'picks'", error("max-tier: {by: picks, 1: a}"));
        assertEquals("npcs.yml: max-tier.six isn't a row: rows are numbers from 1 to 5", error("max-tier: {six: a}"));
        assertEquals("npcs.yml: max-tier.6 isn't a row: rows are numbers from 1 to 5", error("max-tier: {6: a}"));
        assertEquals("npcs.yml: max-tier.0 isn't a row: rows are numbers from 1 to 5", error("max-tier: {0: a}"));
        assertEquals("npcs.yml: max-tier needs at least one row, like 1: ...", error("max-tier: {by: tier}"));
        assertEquals("npcs.yml: max-tier.2 must be text", error("max-tier: {2: [a]}"));
    }

    @Test
    void tablesRefuseNoRowsAndRowsBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new Steps<>(Basis.LEVEL, Map.<Integer, String>of()));
        assertThrows(IllegalArgumentException.class, () -> new Steps<>(Basis.LEVEL, Map.of(0, "a")));
    }

    @Test
    void equalTablesAreEqual() {
        assertEquals(tiers("max-tier: {1: a, 4: b}"), tiers("max-tier: {by: level, 4: b, 1: a}"));
        assertEquals("{by: level, {1=a, 4=b}}", tiers("max-tier: {1: a, 4: b}").toString());
    }
}
