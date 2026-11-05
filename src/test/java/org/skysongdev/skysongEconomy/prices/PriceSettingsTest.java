package org.skysongdev.skysongEconomy.prices;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceSettingsTest {
    private static PriceSettings parse(String yaml) {
        return PriceSettings.parse(YamlFiles.parse("prices.yml", new StringReader(yaml)));
    }

    @Test
    void truncatesNeverRounds() {
        PriceSettings settings = new PriceSettings(1);

        assertEquals("4.0", settings.truncate(new BigDecimal("4.09")).toPlainString());
        assertEquals("4.9", settings.truncate(new BigDecimal("4.99")).toPlainString());
        assertEquals("-1.2", settings.truncate(new BigDecimal("-1.29")).toPlainString());
        assertEquals("3", new PriceSettings(0).truncate(new BigDecimal("3.99")).toPlainString());
    }

    @Test
    void readsDecimalsWithADefaultOfOne() {
        assertEquals(1, parse("{}").decimals());
        assertEquals(2, parse("decimals: 2").decimals());
        assertThrows(ConfigException.class, () -> parse("decimals: 5"));
        assertThrows(ConfigException.class, () -> parse("decimals: -1"));
    }
}
