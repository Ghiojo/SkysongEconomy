package org.skysongdev.skysongEconomy.business.shift;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftSettingsParserTest {
    private static final BusinessTypeRegistry TYPES = new BusinessTypeRegistry(List.of(
            new BusinessType("gathering", "Gathering", Set.of("mining"), false),
            new BusinessType("crafting", "Crafting", Set.of("smithing"), false),
            new BusinessType("service", "Service", Set.of("medical"), true)));

    private final List<String> warnings = new ArrayList<>();
    private final ShiftSettingsParser parser = new ShiftSettingsParser(TYPES, warnings::add);

    private ShiftSettings parse(String yaml) {
        return parser.parse(YamlFiles.parse("shifts.yml", new StringReader(yaml)));
    }

    @Test
    void bundledFilePaysEveryTypeAtEveryTier() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/shifts.yml")), StandardCharsets.UTF_8)) {
            ShiftSettings settings = parser.parse(YamlFiles.parse(ShiftSettingsParser.FILE, reader));

            assertEquals(Duration.ofMinutes(30), settings.duration());
            assertEquals(1, settings.downtimeCost());
            assertEquals(ShiftSettingsParser.DEFAULT_MESSAGE, settings.defaultMessage());
            assertEquals(1, settings.pay("gathering", 1).tokens(), "gathering pays tokens");
            assertEquals(0, settings.pay("crafting", 5).tokens());
            assertTrue(warnings.isEmpty(), warnings.toString());
        }
    }

    @Test
    void readsPayByTypeAndTier() {
        ShiftSettings settings = parse("""
                duration-minutes: 5
                downtime-cost: 0
                pay:
                  gathering: {1: {money: 2.5, tokens: 3}}
                """);

        assertEquals(Duration.ofMinutes(5), settings.duration());
        assertEquals(0, settings.downtimeCost());
        assertEquals("2.5", settings.pay("gathering", 1).money().toPlainString());
        assertEquals(3, settings.pay("gathering", 1).tokens());
    }

    @Test
    void missingPayPaysNothingAndWarns() {
        ShiftSettings settings = parse("pay: {gathering: {1: {money: 1}}}");

        assertTrue(settings.pay("gathering", 2).isNothing());
        assertTrue(settings.pay("crafting", 1).isNothing());
        assertEquals(3, warnings.size(), "one warning per type: " + warnings);
    }

    @Test
    void unknownTypesAreIgnoredWithAWarning() {
        parse("pay: {fishing: {1: {money: 1}}}");

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("fishing")), warnings.toString());
    }

    @Test
    void refusesBadValues() {
        assertThrows(ConfigException.class, () -> parse("duration-minutes: 0"));
        assertThrows(ConfigException.class, () -> parse("downtime-cost: -1"));
        assertThrows(ConfigException.class, () -> parse("pay: {gathering: {6: {money: 1}}}"));
        assertThrows(ConfigException.class, () -> parse("pay: {gathering: {1: {money: -1}}}"));
        assertThrows(ConfigException.class, () -> parse("pay: {gathering: {1: {tokens: -1}}}"));
    }
}
