package org.skysongdev.skysongEconomy.journal;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalSettingsTest {
    private static JournalSettings parse(String yaml) {
        return JournalSettings.parse(YamlFiles.parse(JournalSettings.FILE, new StringReader(yaml)));
    }

    @Test
    void recordsEverythingForeverByDefault() {
        assertEquals(JournalSettings.DEFAULT, parse("{}"));
        assertEquals(new JournalSettings(true, 0), parse("enabled: true\nkeep-days: 0"));
    }

    @Test
    void readsOffAndAKeepTime() {
        assertEquals(new JournalSettings(false, 90), parse("enabled: false\nkeep-days: 90"));
    }

    @Test
    void refusesANegativeKeepTime() {
        assertThrows(ConfigException.class, () -> parse("keep-days: -1"));
    }
}
