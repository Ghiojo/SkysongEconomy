package org.skysongdev.skysongEconomy.business.access;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessDefaultsTest {
    private static AccessDefaults parse(String yaml) {
        return AccessDefaults.parse(YamlFiles.parse("access.yml", new StringReader(yaml)));
    }

    @Test
    void bundledFileLetsWorkersTrainAndSellForFree() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/access.yml")), StandardCharsets.UTF_8)) {
            AccessDefaults defaults = AccessDefaults.parse(YamlFiles.parse(AccessDefaults.FILE, reader));

            assertEquals(Set.of(Permission.TRAIN, Permission.SELL_EXPORTS), defaults.workers().permissions());
            assertEquals(0, defaults.workers().trainingPrice().compareTo(BigDecimal.ZERO));
            assertEquals(Set.of(), defaults.everyone().permissions());
            assertEquals(0, defaults.everyone().trainingPrice().compareTo(BigDecimal.TEN));
            assertEquals(8, defaults.maxCustomGroups());
        }
    }

    @Test
    void anEmptyFileUsesTheSameDefaults() {
        AccessDefaults defaults = parse("{}");

        assertEquals(Set.of(Permission.TRAIN, Permission.SELL_EXPORTS), defaults.workers().permissions());
        assertEquals(Set.of(), defaults.everyone().permissions());
        assertEquals(8, defaults.maxCustomGroups());
    }

    @Test
    void readsPermissionsAndPrices() {
        AccessDefaults defaults = parse("""
                max-custom-groups: 2
                defaults:
                  workers: {permissions: [order-imports], training-price: 2.5}
                  everyone: {permissions: [train], training-price: 40}
                """);

        assertEquals(Set.of(Permission.ORDER_IMPORTS), defaults.workers().permissions());
        assertEquals("2.5", defaults.workers().trainingPrice().toPlainString());
        assertEquals(Set.of(Permission.TRAIN), defaults.everyone().permissions());
        assertEquals("40", defaults.everyone().trainingPrice().toPlainString());
        assertEquals(2, defaults.maxCustomGroups());
    }

    @Test
    void refusesUnknownPermissions() {
        ConfigException error = assertThrows(ConfigException.class,
                () -> parse("defaults: {everyone: {permissions: [fly]}}"));
        assertEquals(true, error.getMessage().contains("fly"), error.getMessage());
    }

    @Test
    void refusesNegativeNumbers() {
        assertThrows(ConfigException.class, () -> parse("max-custom-groups: -1"));
        assertThrows(ConfigException.class, () -> parse("defaults: {workers: {training-price: -5}}"));
    }
}
