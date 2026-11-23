package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.calendar.CalendarNamesTest;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.loot.YieldEffect;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.calendar.FixedCalendar.dayOf;

class CalendarModifierParserTest {
    private static final CalendarNames NAMES = CalendarNamesTest.atmos();
    private final List<String> warnings = new ArrayList<>();
    private final CalendarModifierParser parser = new CalendarModifierParser(NAMES,
            new CalendarModifierParser.KnownSources(Set.of("herbs"), Set.of("foraging"), Set.of("crop"), Set.of("wheat")),
            warnings::add);

    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("nodes/modifiers/test.yml", new StringReader(text));
    }

    @Test
    void readsAWholeFile() {
        CalendarModifier modifier = parser.parse("winter-crops", yaml("""
                name: "<aqua>Winter frost"
                when:
                  seasons: ["Vinse'er"]
                  dates:
                    - {from: "Saimiri 40", to: "Saimiri 45"}
                    - {day: "Celestine 1"}
                  festival: true
                applies-to:
                  farm-types: [crop]
                  random-types: [foraging]
                  tiers: [1, 2]
                effect: "max-50%; rolls-1; chance-50%"
                """));

        assertEquals("<aqua>Winter frost", modifier.name());
        assertEquals(Set.of("vinse_er"), modifier.window().seasonIds());
        assertTrue(modifier.window().contains(dayOf("saimiri", 42), NAMES));
        assertTrue(modifier.window().contains(dayOf("celestine", 1), NAMES));
        assertFalse(modifier.window().contains(dayOf("celestine", 2), NAMES));
        assertTrue(modifier.window().festival());
        assertEquals(Set.of("crop"), modifier.target().farmTypes());
        assertEquals(Set.of(1, 2), modifier.target().tiers());
        assertEquals(YieldEffect.parse("max-50%; rolls-1; chance-50%"), modifier.effect());
        assertFalse(modifier.dormant());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void dormantStopsHarvestsInsteadOfChangingThem() {
        CalendarModifier modifier = parser.parse("sleep", yaml("""
                when: {seasons: [vinse_er]}
                applies-to: {farm-resources: [wheat]}
                effect: Dormant
                """));

        assertTrue(modifier.dormant());
        assertEquals(YieldEffect.NONE, modifier.effect());
        assertEquals("sleep", modifier.name(), "the name defaults to the file name");
    }

    @Test
    void unknownSeasonsAndMonthsNameTheOnesThatExist() {
        ConfigException season = assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {seasons: [winter]}
                applies-to: {farm-types: [crop]}
                effect: "max-1"
                """)));
        assertTrue(season.getMessage().contains("Florescia, Su'untah, Altrue, Vinse'er"), season.getMessage());

        ConfigException month = assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {dates: [{day: "January 1"}]}
                applies-to: {farm-types: [crop]}
                effect: "max-1"
                """)));
        assertTrue(month.getMessage().contains("Celestine"), month.getMessage());
    }

    @Test
    void refusesFilesThatSayNothing() {
        assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                applies-to: {farm-types: [crop]}
                effect: "max-1"
                """)), "no when");
        assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {festival: true}
                effect: "max-1"
                """)), "no applies-to");
        assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {festival: true}
                applies-to: {farm-types: [crop]}
                """)), "no effect");
        assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {festival: true}
                applies-to: {farm-types: [crop]}
                effect: "luck+1"
                """)), "a bad effect");
        assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {dates: [{from: "Rana 1"}]}
                applies-to: {farm-types: [crop]}
                effect: "max-1"
                """)), "a range without an end");
        assertThrows(ConfigException.class, () -> parser.parse("bad", yaml("""
                when: {festival: true}
                applies-to: {farm-types: [crop], tiers: [6]}
                effect: "max-1"
                """)), "a tier above 5");
    }

    @Test
    void namesThatDidNotLoadOnlyWarn() {
        CalendarModifier modifier = parser.parse("later", yaml("""
                when: {festival: true}
                applies-to: {random-tables: [fishing], nodes: [any_node_id]}
                effect: "rolls+1"
                """));

        assertEquals(Set.of("fishing"), modifier.target().randomTables());
        assertEquals(1, warnings.size(), "nodes aren't checked, they live in the database");
        assertTrue(warnings.getFirst().contains("fishing"));
    }
}
