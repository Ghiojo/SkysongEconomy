package org.skysongdev.skysongEconomy.business.npc;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSettingsParserTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical")::contains;

    private final List<String> warnings = new ArrayList<>();

    private NpcSettings parse(String yaml) {
        return new NpcSettingsParser(SKILLS, warnings::add)
                .parse(YamlFiles.parse(NpcSettingsParser.FILE, new StringReader(yaml)));
    }

    private String problem(String yaml) {
        return assertThrows(ConfigException.class, () -> parse(yaml)).getMessage();
    }

    @Test
    void readsWhereNpcsAppearAndEachSkillsSlotsAndTemplate() {
        NpcSettings settings = parse("""
                spawn-at: business
                spawn-radius: 5
                skills:
                  martial:
                    slots: "{level}"
                    template: business_guard
                  economical:
                    slots: {by: level, 1: 1, 3: 2, 5: 3}
                    template: business_clerk
                """);

        assertEquals(SpawnAt.BUSINESS, settings.spawnAt());
        assertEquals(5, settings.spawnRadius());
        assertEquals(List.of("martial", "economical"), List.copyOf(settings.skills().skills()));
        assertEquals(Optional.of("business_guard"), settings.template(SlotSource.skill("martial")));
        assertEquals(2, settings.skills().of("martial").orElseThrow().slots().at(new BusinessScale(2, 4)));
        assertEquals(2, settings.skills().of("economical").orElseThrow().slots().at(new BusinessScale(4, 4)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void theBundledFileIsValidAndGivesNobodyNpcs() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/npcs.yml")), StandardCharsets.UTF_8)) {
            NpcSettings settings = new NpcSettingsParser(SKILLS, warnings::add)
                    .parse(YamlFiles.parse(NpcSettingsParser.FILE, reader));

            assertEquals(SpawnAt.PLAYER, settings.spawnAt());
            assertEquals(NpcSettings.DEFAULT_SPAWN_RADIUS, settings.spawnRadius());
            assertTrue(settings.skills().skills().isEmpty());
            assertTrue(warnings.isEmpty());
        }
    }

    @Test
    void anEmptyFileGivesNobodyNpcs() {
        NpcSettings settings = parse("{}");

        assertEquals(SpawnAt.PLAYER, settings.spawnAt());
        assertEquals(NpcSettings.DEFAULT_SPAWN_RADIUS, settings.spawnRadius());
        assertTrue(settings.skills().skills().isEmpty());
        assertEquals(Optional.empty(), settings.template(SlotSource.skill("martial")));
    }

    @Test
    void unknownSkillsAreSkippedWithAWarning() {
        NpcSettings settings = parse("""
                skills:
                  juggling: {slots: 1, template: clown}
                  martial: {slots: 1, template: business_guard}
                """);

        assertEquals(List.of("martial"), List.copyOf(settings.skills().skills()));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("juggling"), warnings.get(0));
    }

    @Test
    void aSkillNeedsSlotsAndATemplate() {
        assertTrue(problem("skills: {martial: {template: business_guard}}").contains("slots"));
        assertTrue(problem("skills: {martial: {slots: 1}}").contains("template"));
        assertTrue(problem("skills: {martial: {slots: \"{levle}\", template: business_guard}}").contains("slots"));
    }

    @Test
    void refusesAnUnknownPlaceAndANegativeRadius() {
        assertTrue(problem("spawn-at: moon").contains("player or business"));
        assertTrue(problem("spawn-radius: -1").contains("spawn-radius"));
    }
}
