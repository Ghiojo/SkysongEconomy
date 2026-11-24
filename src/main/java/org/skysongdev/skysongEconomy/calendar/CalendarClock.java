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

        @Override
        public void whenDayChanges(Runnable action) {
            // No calendar, no new days.
        }
    };

    /** @return today's IC day; empty without a calendar */
    Optional<CalendarDay> today();

    /** @return the calendar's seasons and months; empty without a calendar */
    Optional<CalendarNames> names();

    /** @return today's full IC date, with its year; empty without a calendar */
    default Optional<CalendarDate> date() {
        return Optional.empty();
    }

    /**
     * Runs {@code action} on the main thread whenever a new IC day starts, and when the calendar
     * reloads. Without a calendar it never runs.
     */
    void whenDayChanges(Runnable action);
}
