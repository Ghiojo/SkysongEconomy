package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.calendar.CalendarNamesTest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.calendar.FixedCalendar.dayOf;

class CalendarWindowTest {
    private static final CalendarNames NAMES = CalendarNamesTest.atmos();

    private static int day(String text) {
        CalendarNames.MonthDay day = NAMES.monthDay(text);
        return NAMES.dayOfYear(day.month(), day.day());
    }

    @Test
    void aSeasonHoldsEveryDayOfIt() {
        CalendarWindow winter = new CalendarWindow(Set.of("vinse_er"), List.of(), false);

        assertTrue(winter.contains(dayOf("rana", 1), NAMES));
        assertTrue(winter.contains(dayOf("litoria", 45), NAMES));
        assertTrue(winter.contains(CalendarDay.festival("vinse_er", 2), NAMES), "the festival is in Vinse'er");
        assertFalse(winter.contains(dayOf("celestine", 1), NAMES));
    }

    @Test
    void aDateRangeHoldsBothEnds() {
        CalendarWindow frost = new CalendarWindow(Set.of(),
                List.of(new CalendarWindow.DateRange(day("Rana 10"), day("Litoria 5"))), false);

        assertFalse(frost.contains(dayOf("rana", 9), NAMES));
        assertTrue(frost.contains(dayOf("rana", 10), NAMES));
        assertTrue(frost.contains(dayOf("litoria", 5), NAMES));
        assertFalse(frost.contains(dayOf("litoria", 6), NAMES));
    }

    @Test
    void aRangeCanWrapPastTheNewYearButSkipsTheFestival() {
        CalendarWindow turn = new CalendarWindow(Set.of(),
                List.of(new CalendarWindow.DateRange(day("Litoria 40"), day("Celestine 5"))), false);

        assertTrue(turn.contains(dayOf("litoria", 45), NAMES));
        assertTrue(turn.contains(dayOf("celestine", 1), NAMES));
        assertTrue(turn.contains(dayOf("celestine", 5), NAMES));
        assertFalse(turn.contains(dayOf("idalia", 1), NAMES));
        assertFalse(turn.contains(CalendarDay.festival("vinse_er", 1), NAMES));
    }

    @Test
    void theFestivalOnlyHoldsFestivalDays() {
        CalendarWindow festival = new CalendarWindow(Set.of(), List.of(), true);

        assertTrue(festival.contains(CalendarDay.festival("vinse_er", 1), NAMES));
        assertFalse(festival.contains(dayOf("litoria", 45), NAMES));
    }

    @Test
    void needsSomething() {
        assertThrows(IllegalArgumentException.class, () -> new CalendarWindow(Set.of(), List.of(), false));
    }
}
