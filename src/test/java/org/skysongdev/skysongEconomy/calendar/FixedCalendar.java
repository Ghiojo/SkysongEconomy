package org.skysongdev.skysongEconomy.calendar;

import java.util.Optional;

/** A calendar for tests: SkysongCalendar's seasons and months, on a day the test picks. */
public final class FixedCalendar implements CalendarClock {
    private CalendarDay today;

    public FixedCalendar(CalendarDay today) {
        this.today = today;
    }

    /** @return a calendar on that day of that month, in its season */
    public static FixedCalendar on(String monthId, int day) {
        return new FixedCalendar(dayOf(monthId, day));
    }

    /** @return that day of that month, in its season */
    public static CalendarDay dayOf(String monthId, int day) {
        CalendarMonth month = CalendarNamesTest.atmos().month(monthId).orElseThrow();
        return CalendarDay.of(month.seasonId(), month.id(), day);
    }

    public void set(CalendarDay day) {
        this.today = day;
    }

    @Override
    public Optional<CalendarDay> today() {
        return Optional.of(today);
    }

    @Override
    public Optional<CalendarNames> names() {
        return Optional.of(CalendarNamesTest.atmos());
    }

    @Override
    public void whenDayChanges(Runnable action) {
        // Tests change the day with set.
    }
}
