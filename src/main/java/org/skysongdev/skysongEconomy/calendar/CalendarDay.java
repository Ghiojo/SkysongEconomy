package org.skysongdev.skysongEconomy.calendar;

import java.util.Objects;
import java.util.Optional;

/**
 * One IC day, as the plugin's rules need it.
 *
 * @param seasonId the id of the season it falls in
 * @param monthId  the id of the month it falls in; empty on the festival days that close the year
 * @param day      the day of the month or of the festival, counting from 1
 */
public record CalendarDay(String seasonId, Optional<String> monthId, int day) {
    public CalendarDay {
        Objects.requireNonNull(seasonId, "seasonId");
        Objects.requireNonNull(monthId, "monthId");
        if (day < 1) {
            throw new IllegalArgumentException("day counts from 1: " + day);
        }
    }

    /** @return a day of a month */
    public static CalendarDay of(String seasonId, String monthId, int day) {
        return new CalendarDay(seasonId, Optional.of(monthId), day);
    }

    /** @return a festival day */
    public static CalendarDay festival(String seasonId, int day) {
        return new CalendarDay(seasonId, Optional.empty(), day);
    }

    /** @return whether it is one of the festival days that close the year */
    public boolean isFestival() {
        return monthId.isEmpty();
    }
}
