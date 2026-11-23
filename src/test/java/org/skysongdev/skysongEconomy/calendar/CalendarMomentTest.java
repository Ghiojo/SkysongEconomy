package org.skysongdev.skysongEconomy.calendar;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarMomentTest {
    private static final CalendarNames NAMES = CalendarNamesTest.atmos();
    private static final Optional<CalendarDay> TODAY = Optional.of(CalendarDay.of("altrue", "lontra", 7));

    @Test
    void readsTodayFestivalSeasonsAndDays() {
        assertEquals(new CalendarMoment.Day(TODAY.get()), CalendarMoment.parse("Today", NAMES, TODAY));
        assertEquals(new CalendarMoment.Day(CalendarDay.festival("vinse_er", 1)), CalendarMoment.parse("festival", NAMES, TODAY));
        assertEquals(new CalendarMoment.Season("vinse_er"), CalendarMoment.parse("Vinse'er", NAMES, TODAY));
        assertEquals(new CalendarMoment.Day(CalendarDay.of("vinse_er", "rana", 10)), CalendarMoment.parse("rana 10", NAMES, TODAY));
    }

    @Test
    void anythingElseNamesWhatCanBeWritten() {
        IllegalArgumentException wrong = assertThrows(IllegalArgumentException.class,
                () -> CalendarMoment.parse("winter", NAMES, TODAY));
        assertTrue(wrong.getMessage().contains("Florescia"), wrong.getMessage());
        assertThrows(IllegalArgumentException.class, () -> CalendarMoment.parse("today", NAMES, Optional.empty()));
    }
}
