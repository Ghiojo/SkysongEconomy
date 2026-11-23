package org.skysongdev.skysongEconomy.calendar;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarNamesTest {
    /** A calendar shaped like SkysongCalendar's, written here only as test data. */
    static CalendarNames atmos() {
        CalendarName florescia = new CalendarName("florescia", "Florescia");
        CalendarName suUntah = new CalendarName("su_untah", "Su'untah");
        CalendarName altrue = new CalendarName("altrue", "Altrue");
        CalendarName vinseEr = new CalendarName("vinse_er", "Vinse'er");
        return new CalendarNames(List.of(florescia, suUntah, altrue, vinseEr), List.of(
                new CalendarMonth(new CalendarName("celestine", "Celestine"), "florescia"),
                new CalendarMonth(new CalendarName("idalia", "Idalia"), "florescia"),
                new CalendarMonth(new CalendarName("dasyurus", "Dasyurus"), "su_untah"),
                new CalendarMonth(new CalendarName("ramphastos", "Ramphastos"), "su_untah"),
                new CalendarMonth(new CalendarName("lontra", "Lontra"), "altrue"),
                new CalendarMonth(new CalendarName("saimiri", "Saimiri"), "altrue"),
                new CalendarMonth(new CalendarName("rana", "Rana"), "vinse_er"),
                new CalendarMonth(new CalendarName("litoria", "Litoria"), "vinse_er")), 45, "vinse_er");
    }

    @Test
    void findsSeasonsByIdOrDisplayNameIgnoringCase() {
        CalendarNames names = atmos();

        assertEquals("vinse_er", names.season("vinse_er").orElseThrow().id());
        assertEquals("vinse_er", names.season("Vinse'er").orElseThrow().id());
        assertEquals("vinse_er", names.season(" VINSE'ER ").orElseThrow().id());
        assertEquals(Optional.empty(), names.season("winter"));
    }

    @Test
    void findsMonthsWithTheirSeason() {
        CalendarMonth rana = atmos().month("rana").orElseThrow();

        assertEquals("rana", rana.id());
        assertEquals("vinse_er", rana.seasonId());
        assertTrue(atmos().month("Ramphastos").isPresent());
        assertFalse(atmos().month("January").isPresent());
    }

    @Test
    void theSeasonAfterTheLastIsTheFirst() {
        CalendarNames names = atmos();

        assertEquals("su_untah", names.seasonAfter("florescia").orElseThrow().id());
        assertEquals("florescia", names.seasonAfter("vinse_er").orElseThrow().id());
        assertEquals(Optional.empty(), names.seasonAfter("winter"));
    }

    @Test
    void listsNamesForWarnings() {
        assertEquals("Florescia, Su'untah, Altrue, Vinse'er", atmos().seasonList());
        assertTrue(atmos().monthList().startsWith("Celestine, Idalia"));
    }

    @Test
    void needsSeasonsMonthsAndAMonthLength() {
        CalendarNames names = atmos();

        assertThrows(IllegalArgumentException.class,
                () -> new CalendarNames(List.of(), names.months(), 45, "vinse_er"));
        assertThrows(IllegalArgumentException.class,
                () -> new CalendarNames(names.seasons(), names.months(), 0, "vinse_er"));
    }

    @Test
    void idsComeFromConstantNames() {
        assertEquals("vinse_er", CalendarName.idOf("VINSE_ER"));
    }
}
