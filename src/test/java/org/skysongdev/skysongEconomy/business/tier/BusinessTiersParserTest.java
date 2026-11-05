package org.skysongdev.skysongEconomy.business.tier;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessTiersParserTest {
    private final BusinessTiersParser parser = new BusinessTiersParser();

    private BusinessTiers parse(String yaml) {
        return parser.parse(YamlFiles.parse("tiers.yml", new StringReader(yaml)));
    }

    @Test
    void bundledDefaultsDefineEveryTier() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/tiers.yml")), StandardCharsets.UTF_8)) {
            BusinessTiers tiers = parser.parse(YamlFiles.parse(BusinessTiersParser.FILE, reader));
            assertEquals(2, tiers.get(1).maxWorkers());
            assertEquals(6, tiers.get(5).maxWorkers());
        }
    }

    @Test
    void readsWorkersPerTier() {
        BusinessTiers tiers = parse("tiers: {1: {workers: 0}, 2: {workers: 1}, 3: {workers: 1}, 4: {workers: 2}, 5: {workers: 9}}");

        assertEquals(0, tiers.get(1).maxWorkers());
        assertEquals(9, tiers.get(5).maxWorkers());
    }

    @Test
    void everyTierMustBeDefined() {
        ConfigException error = assertThrows(ConfigException.class,
                () -> parse("tiers: {1: {workers: 1}, 2: {workers: 1}, 3: {workers: 1}, 4: {workers: 1}}"));
        assertEquals("tiers.yml: tiers.5 is missing: every tier from 1 to 5 must be defined", error.getMessage());
    }

    @Test
    void refusesUnknownTiersAndMissingWorkers() {
        assertThrows(ConfigException.class, () -> parse("tiers: {6: {workers: 1}}"));
        assertThrows(ConfigException.class, () -> parse("tiers: {one: {workers: 1}}"));
        assertThrows(ConfigException.class, () -> parse("tiers: {1: {}}"));
        assertThrows(ConfigException.class, () -> parse("tiers: {1: {workers: -1}}"));
    }
}
