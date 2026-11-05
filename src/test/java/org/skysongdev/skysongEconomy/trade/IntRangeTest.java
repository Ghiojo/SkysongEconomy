package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntRangeTest {
    private static ConfigSection yaml(String text) {
        return YamlFiles.parse("test.yml", new StringReader(text));
    }

    @Test
    void readsANumberOrARange() {
        assertEquals(IntRange.exactly(3), IntRange.parse(yaml("a: 3"), "a", null));
        assertEquals(new IntRange(1, 3), IntRange.parse(yaml("a: \"1-3\""), "a", null));
        assertEquals(new IntRange(2, 5), IntRange.parse(yaml("a: 2 - 5"), "a", null));
    }

    @Test
    void usesTheFallbackOrRequiresTheKey() {
        assertEquals(IntRange.exactly(1), IntRange.parse(yaml("b: 2"), "a", IntRange.exactly(1)));
        assertThrows(ConfigException.class, () -> IntRange.parse(yaml("b: 2"), "a", null));
    }

    @Test
    void refusesBrokenRanges() {
        for (String bad : new String[]{"a: \"3-1\"", "a: -2", "a: lots", "a: \"1-\""}) {
            ConfigException error = assertThrows(ConfigException.class, () -> IntRange.parse(yaml(bad), "a", null), bad);
            assertTrue(error.getMessage().contains("test.yml: a"), error.getMessage());
        }
    }

    @Test
    void rollsStayInsideTheRange() {
        IntRange range = new IntRange(1, 3);
        Random random = new Random(7);
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 200; i++) {
            int roll = range.roll(random);
            assertTrue(roll >= 1 && roll <= 3, "rolled " + roll);
            seen[roll] = true;
        }
        assertTrue(seen[1] && seen[2] && seen[3], "every value comes up");
    }

    @Test
    void averageIsTheMiddle() {
        assertEquals(new BigDecimal("2.0"), new IntRange(1, 3).average());
        assertEquals(new BigDecimal("1.5"), new IntRange(1, 2).average());
    }
}
