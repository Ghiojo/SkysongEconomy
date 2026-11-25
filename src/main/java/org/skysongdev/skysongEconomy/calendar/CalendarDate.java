package org.skysongdev.skysongEconomy.calendar;

import java.util.Objects;

/**
 * Today's full IC date, for stamping records.
 *
 * @param year      the IC year
 * @param period    the month's or festival's display name, as SkysongCalendar spells it, such as {@code Celestine}
 * @param day       the day of the month or festival, counting from 1
 * @param dayOfYear the day of the year, counting from 0 (Celestine 1 is 0), as SkysongCalendar counts it
 * @param festival  whether it is one of the festival days that close the year
 */
public record CalendarDate(int year, String period, int day, int dayOfYear, boolean festival) {
    public CalendarDate {
        Objects.requireNonNull(period, "period");
        if (day < 1 || dayOfYear < 0) {
            throw new IllegalArgumentException("day counts from 1 and dayOfYear from 0: " + day + ", " + dayOfYear);
        }
    }
}
