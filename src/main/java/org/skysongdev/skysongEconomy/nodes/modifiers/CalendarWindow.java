package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * When a calendar modifier is on: any of its seasons, any of its date ranges, or the festival.
 *
 * @param seasonIds the seasons it is on for, by id
 * @param dates     ranges of month days, every IC year; a range may wrap past the new year
 * @param festival  whether it is on during the festival days that close the year
 */
public record CalendarWindow(Set<String> seasonIds, List<DateRange> dates, boolean festival) {
    public CalendarWindow {
        seasonIds = Set.copyOf(seasonIds);
        dates = List.copyOf(dates);
        if (seasonIds.isEmpty() && dates.isEmpty() && !festival) {
            throw new IllegalArgumentException("A window needs a season, a date range or the festival");
        }
    }

    /** @return whether the window holds that day of that calendar */
    public boolean contains(CalendarDay day, CalendarNames names) {
        if (seasonIds.contains(day.seasonId())) {
            return true;
        }
        if (day.isFestival()) {
            return festival;
        }
        OptionalInt dayOfYear = names.dayOfYear(day);
        return dayOfYear.isPresent() && dates.stream().anyMatch(range -> range.contains(dayOfYear.getAsInt()));
    }

    /**
     * A range of month days, both ends included, by {@link CalendarNames#dayOfYear} numbers. When
     * {@code from} is after {@code to} it runs past the end of the year and on from the start, so
     * the festival days in between are never in it (use {@code festival: true} for those).
     *
     * @param from the first day
     * @param to   the last day
     */
    public record DateRange(int from, int to) {
        public DateRange {
            if (from < 0 || to < 0) {
                throw new IllegalArgumentException("days of the year count from 0");
            }
        }

        public boolean contains(int dayOfYear) {
            return from <= to ? dayOfYear >= from && dayOfYear <= to : dayOfYear >= from || dayOfYear <= to;
        }
    }
}
