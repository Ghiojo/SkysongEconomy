package org.skysongdev.skysongEconomy.business.scale;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillSectionsTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical", "medical")::contains;

    private final List<String> warnings = new ArrayList<>();

    private SkillSections<Amount> slots(String fileName, String yaml) {
        ConfigSection root = YamlFiles.parse(fileName, new StringReader(yaml));
        return SkillSections.read(root, SKILLS, warnings::add,
                (skill, section) -> AmountParser.require(section, "slots"));
    }

    @Test
    void aListedSkillHasTheFunctionalityWithItsOwnSettings() {
        SkillSections<Amount> npcs = slots("npcs.yml", """
                skills:
                  martial:
                    slots: "{level}"
                  Economical:
                    slots: {by: tier, 1: 1, 3: 2}
                """);

        assertEquals(List.of("martial", "economical"), List.copyOf(npcs.skills()));
        assertTrue(npcs.has("martial"));
        assertFalse(npcs.has("medical"));
        assertEquals(Optional.empty(), npcs.of("medical"));
        assertEquals(3, npcs.of("martial").orElseThrow().at(new BusinessScale(3, 3)));
        assertEquals(2, npcs.of("economical").orElseThrow().at(new BusinessScale(1, 4)));
        assertEquals(npcs.skills(), npcs.all().keySet());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void functionalitiesDoNotExcludeEachOther() {
        SkillSections<Amount> npcs = slots("npcs.yml", "skills: {martial: {slots: 1}, economical: {slots: 1}}");
        SkillSections<Amount> modules = slots("modules.yml", "skills: {martial: {slots: 2}}");

        assertTrue(npcs.has("martial") && modules.has("martial"), "one skill can have both");
        assertTrue(npcs.has("economical"));
        assertFalse(modules.has("economical"), "and another only one of them");
    }

    @Test
    void unknownSkillsAreSkippedWithAWarning() {
        SkillSections<Amount> npcs = slots("npcs.yml", "skills: {smithing: {slots: 1}, martial: {slots: 1}}");

        assertEquals(Set.of("martial"), npcs.skills());
        assertEquals(List.of("npcs.yml: skills: unknown skill 'smithing' was skipped"), warnings);
    }

    @Test
    void aFileWithoutSkillsGivesTheFunctionalityToNone() {
        assertTrue(slots("npcs.yml", "other: 1").skills().isEmpty());
        assertTrue(SkillSections.<Amount>none().skills().isEmpty());
        assertFalse(SkillSections.<Amount>none().has("martial"));
    }

    @Test
    void mistakesNameTheSkill() {
        assertEquals("npcs.yml: skills.martial.slots is required", assertThrows(ConfigException.class,
                () -> slots("npcs.yml", "skills: {martial: {}}")).getMessage());
        assertEquals("npcs.yml: skills.martial must be a section", assertThrows(ConfigException.class,
                () -> slots("npcs.yml", "skills: {martial: 3}")).getMessage());
        assertEquals("npcs.yml: skills.Martial is listed twice", assertThrows(ConfigException.class,
                () -> slots("npcs.yml", "skills: {martial: {slots: 1}, Martial: {slots: 2}}")).getMessage());
    }
}
