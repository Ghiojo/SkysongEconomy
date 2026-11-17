package org.skysongdev.skysongEconomy.upkeep;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpkeepSettingsParserTest {
    private final List<String> warnings = new ArrayList<>();
    private final UpkeepSettingsParser parser = new UpkeepSettingsParser(
            Set.of("water_flask", "architite_coin")::contains, Set.of("food")::contains, warnings::add);

    private UpkeepSettings parse(String yaml) {
        return parser.parse(YamlFiles.parse(UpkeepSettingsParser.FILE, new StringReader(yaml)));
    }

    private String problem(String yaml) {
        return assertThrows(ConfigException.class, () -> parse(yaml)).getMessage();
    }

    @Test
    void readsProfilesWithItemAndItemCategoryEntriesAndTemplates() {
        UpkeepSettings settings = parse("""
                check-every: 30
                profiles:
                  guard_rations:
                    every: 1d12h
                    slots: 18
                    cost:
                      - {item: water_flask, amount: 1}
                      - {good-type: Food, amount: 3}
                  hireling:
                    every: 3d
                    cost:
                      - {item: architite_coin, amount: 5}
                templates:
                  Business_Guard: guard_rations
                """);

        assertEquals(Duration.ofSeconds(30), settings.checkEvery());
        UpkeepProfile rations = settings.profile("guard_rations").orElseThrow();
        assertEquals(Duration.ofHours(36), rations.every());
        assertEquals(18, rations.slots());
        assertEquals(List.of(new UpkeepCost(new UpkeepCost.Match.Item("water_flask"), 1),
                new UpkeepCost(new UpkeepCost.Match.GoodType("food"), 3)), rations.cost());
        assertEquals(9, settings.profile("hireling").orElseThrow().slots());
        assertEquals(Optional.of(rations), settings.forTemplate("business_guard"));
        assertEquals(Optional.of(rations), settings.forTemplate("BUSINESS_GUARD"));
        assertEquals(Optional.empty(), settings.forTemplate("guard_dog"));
        assertTrue(warnings.isEmpty(), warnings::toString);
    }

    @Test
    void theBundledFileIsValidAndGivesNobodyAnUpkeep() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/" + UpkeepSettingsParser.FILE)), StandardCharsets.UTF_8)) {
            UpkeepSettings settings = parser.parse(YamlFiles.parse(UpkeepSettingsParser.FILE, reader));

            assertEquals(UpkeepSettings.DEFAULT_CHECK_EVERY, settings.checkEvery());
            assertTrue(settings.profiles().isEmpty());
            assertTrue(settings.templates().isEmpty());
        }
    }

    @Test
    void readsTheNameLineHeightAndRefusesOneTooFarAway() {
        assertEquals(UpkeepSettings.DEFAULT_NAME_LINE_HEIGHT, parse("check-every: 30").nameLineHeight());
        assertEquals(0.5, parse("name-line-height: 0.5").nameLineHeight());
        assertTrue(problem("name-line-height: 5").contains("name-line-height"));
    }

    @Test
    void anEmptyFileGivesNobodyAnUpkeep() {
        UpkeepSettings settings = parse("{}");

        assertTrue(settings.profiles().isEmpty());
        assertEquals(UpkeepSettings.DEFAULT_CHECK_EVERY, settings.checkEvery());
    }

    @Test
    void itemsAndCategoriesThatDoNotExistYetOnlyWarn() {
        parse("""
                profiles:
                  odd:
                    every: 1h
                    cost:
                      - {item: moon_cheese}
                      - {good-type: gems}
                """);

        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("moon_cheese"));
        assertTrue(warnings.get(1).contains("gems"));
    }

    @Test
    void refusesBrokenProfiles() {
        assertTrue(problem("profiles: {p: {cost: [{item: water_flask}]}}").contains("every"));
        assertTrue(problem("profiles: {p: {every: soon, cost: [{item: water_flask}]}}").contains("every"));
        assertTrue(problem("profiles: {p: {every: 1h, slots: 10, cost: [{item: water_flask}]}}").contains("slots"));
        assertTrue(problem("profiles: {p: {every: 1h}}").contains("cost"));
        assertTrue(problem("profiles: {p: {every: 1h, cost: [{amount: 2}]}}").contains("good-type"));
        assertTrue(problem("profiles: {p: {every: 1h, cost: [{item: water_flask, good-type: food}]}}").contains("not both"));
        assertTrue(problem("profiles: {p: {every: 1h, cost: [{item: water_flask, amount: 0}]}}").contains("amount"));
        assertTrue(problem("check-every: 0").contains("check-every"));
    }

    @Test
    void refusesATemplateNamingAMissingProfile() {
        assertTrue(problem("""
                profiles:
                  hireling: {every: 3d, cost: [{item: architite_coin}]}
                templates:
                  business_guard: guard_rations
                """).contains("guard_rations"));
    }
}
