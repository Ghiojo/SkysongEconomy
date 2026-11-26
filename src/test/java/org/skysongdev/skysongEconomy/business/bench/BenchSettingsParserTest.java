package org.skysongdev.skysongEconomy.business.bench;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class BenchSettingsParserTest {
    private final BenchSettingsParser parser = new BenchSettingsParser();

    @Test
    void bundledDefaultsLetBusinessStaffIn() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/benches.yml")), StandardCharsets.UTF_8)) {
            assertTrue(parser.parse(YamlFiles.parse(BenchSettingsParser.FILE, reader)).businessStaffCanUse());
        }
    }

    @Test
    void canBeTurnedOff() {
        assertFalse(parser.parse(YamlFiles.parse("benches.yml", new StringReader("business-staff-can-use: false")))
                .businessStaffCanUse());
    }

    @Test
    void missingKeyLetsStaffIn() {
        assertTrue(parser.parse(YamlFiles.parse("benches.yml", new StringReader(""))).businessStaffCanUse());
    }
}
