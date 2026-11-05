package org.skysongdev.skysongEconomy.business.display;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameplateSettingsParserTest {
    private final NameplateSettingsParser parser = new NameplateSettingsParser();

    private NameplateSettings parse(String yaml) {
        return parser.parse(YamlFiles.parse("block.yml", new StringReader(yaml)));
    }

    @Test
    void bundledDefaultsShowASmallNameplate() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/businesses/block.yml")), StandardCharsets.UTF_8)) {
            NameplateSettings settings = parser.parse(YamlFiles.parse(NameplateSettingsParser.FILE, reader));
            assertTrue(settings.enabled());
            assertEquals(16, settings.viewDistance());
            assertEquals(0.25f, settings.viewRange(), 1e-6, "16 of Minecraft's 64 blocks");
        }
    }

    @Test
    void missingKeysUseDefaults() {
        NameplateSettings settings = parse("");

        assertTrue(settings.enabled());
        assertEquals(0.4, settings.height());
        assertFalse(settings.background());
    }

    @Test
    void readsEveryKey() {
        NameplateSettings settings = parse("nameplate: {enabled: false, height: 1, view-distance: 32, shadow: false, background: true}");

        assertFalse(settings.enabled());
        assertEquals(1.0, settings.height());
        assertEquals(0.5f, settings.viewRange(), 1e-6);
        assertFalse(settings.shadow());
        assertTrue(settings.background());
    }

    @Test
    void refusesOutOfRangeValues() {
        assertThrows(ConfigException.class, () -> parse("nameplate: {height: -1}"));
        assertThrows(ConfigException.class, () -> parse("nameplate: {view-distance: 0}"));
    }
}
