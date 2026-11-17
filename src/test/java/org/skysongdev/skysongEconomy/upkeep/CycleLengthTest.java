package org.skysongdev.skysongEconomy.upkeep;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CycleLengthTest {
    @Test
    void readsEachUnit() {
        assertEquals(Duration.ofSeconds(30), CycleLength.parse("30s"));
        assertEquals(Duration.ofMinutes(15), CycleLength.parse("15m"));
        assertEquals(Duration.ofHours(24), CycleLength.parse("24h"));
        assertEquals(Duration.ofDays(3), CycleLength.parse("3d"));
        assertEquals(Duration.ofDays(14), CycleLength.parse("2w"));
    }

    @Test
    void addsCombinedUnitsAndIgnoresSpacesAndCase() {
        assertEquals(Duration.ofHours(36), CycleLength.parse("1d12h"));
        assertEquals(Duration.ofHours(36), CycleLength.parse("1D 12H"));
    }

    @Test
    void refusesTextThatIsNotALength() {
        assertThrows(IllegalArgumentException.class, () -> CycleLength.parse(""));
        assertThrows(IllegalArgumentException.class, () -> CycleLength.parse("24"));
        assertThrows(IllegalArgumentException.class, () -> CycleLength.parse("h24"));
        assertThrows(IllegalArgumentException.class, () -> CycleLength.parse("1d-2h"));
        assertThrows(IllegalArgumentException.class, () -> CycleLength.parse("1y"));
    }

    @Test
    void refusesZero() {
        assertThrows(IllegalArgumentException.class, () -> CycleLength.parse("0h"));
    }
}
