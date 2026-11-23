package org.skysongdev.skysongEconomy.calendar;

import java.util.Optional;

/**
 * Where the plugin reads the IC calendar. On the server it is
 * {@code integration/skysongcalendar/SkysongCalendarAdapter} when SkysongCalendar is installed, and
 * {@link #NONE} when it isn't; tests use a fixed day.
 */
public interface CalendarClock {
    /** No calendar: SkysongCalendar is missing, disabled or too old. Nothing is ever known. */
    CalendarClock NONE = new CalendarClock() {
        @Override
        public Optional<CalendarDay> today() {
            return Optional.empty();
        }

        @Override
        public Optional<CalendarNames> names() {
            return Optional.empty();
        }
    };

    /** @return today's IC day; empty without a calendar */
    Optional<CalendarDay> today();

    /** @return the calendar's seasons and months; empty without a calendar */
    Optional<CalendarNames> names();
}
