package org.skysongdev.skysongEconomy.calendar;

import java.util.Objects;

/**
 * A month of the IC year.
 *
 * @param name     the month's name
 * @param seasonId the id of the season it belongs to
 */
public record CalendarMonth(CalendarName name, String seasonId) {
    public CalendarMonth {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(seasonId, "seasonId");
    }

    /** @return the month's id, such as {@code rana} */
    public String id() {
        return name.id();
    }
}
