package org.skysongdev.skysongEconomy.journal;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.journal.JournalIcDate;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalIcDateTest {
    @Test
    void nineDaysMakeAWeekCountingFromCelestineFirst() {
        assertEquals(OptionalInt.of(1), JournalIcDate.of(1203, "Celestine", 1, 0, false).week());
        assertEquals(OptionalInt.of(1), JournalIcDate.of(1203, "Celestine", 9, 8, false).week());
        assertEquals(OptionalInt.of(2), JournalIcDate.of(1203, "Celestine", 10, 9, false).week());
        assertEquals(OptionalInt.of(40), JournalIcDate.of(1203, "Litoria", 45, 359, false).week());
    }

    @Test
    void festivalDaysBelongToNoWeek() {
        JournalIcDate festival = JournalIcDate.of(1203, "Festival", 1, 360, true);
        assertEquals(OptionalInt.empty(), festival.week());
        assertThrows(IllegalArgumentException.class,
                () -> new JournalIcDate(1203, "Festival", 1, 360, OptionalInt.of(41), true));
    }
}
