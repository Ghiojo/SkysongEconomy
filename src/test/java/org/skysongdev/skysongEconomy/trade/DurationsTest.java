package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationsTest {
    @Test
    void showsTheTwoLargestUnits() {
        assertEquals("3d", Durations.format(Duration.ofDays(3)));
        assertEquals("2d 4h", Durations.format(Duration.ofDays(2).plusHours(4).plusMinutes(30)));
        assertEquals("3h 15m", Durations.format(Duration.ofHours(3).plusMinutes(15)));
        assertEquals("5m", Durations.format(Duration.ofMinutes(5)));
    }

    @Test
    void roundsUpToTheMinute() {
        assertEquals("1m", Durations.format(Duration.ofSeconds(10)));
        assertEquals("0m", Durations.format(Duration.ZERO));
        assertEquals("0m", Durations.format(Duration.ofMinutes(-3)));
    }
}
