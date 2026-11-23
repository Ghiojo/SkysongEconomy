package org.skysongdev.skysongEconomy.calendar;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarDayTest {
    @Test
    void aMonthDayIsNotAFestivalDay() {
        CalendarDay day = CalendarDay.of("vinse_er", "rana", 10);

        assertFalse(day.isFestival());
        assertEquals(Optional.of("rana"), day.monthId());
    }

    @Test
    void aFestivalDayHasNoMonth() {
        CalendarDay day = CalendarDay.festival("vinse_er", 2);

        assertTrue(day.isFestival());
        assertEquals(2, day.day());
    }

    @Test
    void daysCountFromOne() {
        assertThrows(IllegalArgumentException.class, () -> CalendarDay.of("florescia", "celestine", 0));
    }

    @Test
    void withoutACalendarNothingIsKnown() {
        assertEquals(Optional.empty(), CalendarClock.NONE.today());
        assertEquals(Optional.empty(), CalendarClock.NONE.names());
    }
}
