package org.skysongdev.skysongEconomy.api.journal;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * The SkysongCalendar date a journal record happened on. An IC week is 9 IC days: Celestine 1 to 9 is
 * week 1, and the 360 month days make weeks 1 to 40. The festival days that close the year belong to
 * no week.
 *
 * @param year      the IC year
 * @param period    the month's or festival's name as SkysongCalendar spells it, such as {@code Celestine}
 * @param day       the day of the month or festival, counting from 1
 * @param dayOfYear the day of the year, counting from 0 (Celestine 1 is 0), as SkysongCalendar counts it
 * @param week      {@code dayOfYear / 9 + 1} on a month day; empty on a festival day
 * @param festival  whether it is a festival day
 */
public record JournalIcDate(int year, String period, int day, int dayOfYear, OptionalInt week, boolean festival) {
    /** How many IC days make an IC week. */
    public static final int WEEK_LENGTH = 9;

    public JournalIcDate {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(week, "week");
        if (festival == week.isPresent()) {
            throw new IllegalArgumentException("A festival day has no week, and every month day has one");
        }
    }

    /** @return the date, with its week worked out from the day of the year */
    public static JournalIcDate of(int year, String period, int day, int dayOfYear, boolean festival) {
        return new JournalIcDate(year, period, day, dayOfYear,
                festival ? OptionalInt.empty() : OptionalInt.of(dayOfYear / WEEK_LENGTH + 1), festival);
    }
}
