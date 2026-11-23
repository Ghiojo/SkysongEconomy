package org.skysongdev.skysongEconomy.calendar;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * The seasons and months of the installed SkysongCalendar, in calendar order, for config files to
 * name. Names are matched by id or display name, ignoring case, so {@code vinse_er} and
 * {@code Vinse'er} are the same season.
 *
 * @param seasons          the seasons, in the order the year goes through them
 * @param months           the months, in the order of the year
 * @param monthLength      how many days every month has
 * @param festivalSeasonId the season the festival days that close the year belong to
 */
public record CalendarNames(List<CalendarName> seasons, List<CalendarMonth> months, int monthLength,
                            String festivalSeasonId) {
    public CalendarNames {
        seasons = List.copyOf(seasons);
        months = List.copyOf(months);
        Objects.requireNonNull(festivalSeasonId, "festivalSeasonId");
        if (seasons.isEmpty() || months.isEmpty()) {
            throw new IllegalArgumentException("A calendar needs seasons and months");
        }
        if (monthLength < 1) {
            throw new IllegalArgumentException("monthLength must be at least 1: " + monthLength);
        }
    }

    /** @return the season written as {@code text}, by id or display name; empty when there is none */
    public Optional<CalendarName> season(String text) {
        return seasons.stream().filter(season -> season.matches(text)).findFirst();
    }

    /** @return the month written as {@code text}, by id or display name; empty when there is none */
    public Optional<CalendarMonth> month(String text) {
        return months.stream().filter(month -> month.name().matches(text)).findFirst();
    }

    /** @return the season with that id; empty when there is none */
    public Optional<CalendarName> seasonById(String id) {
        return seasons.stream().filter(season -> season.id().equals(id)).findFirst();
    }

    /** @return the season after the one with that id, wrapping round to the first; empty for an unknown id */
    public Optional<CalendarName> seasonAfter(String id) {
        for (int i = 0; i < seasons.size(); i++) {
            if (seasons.get(i).id().equals(id)) {
                return Optional.of(seasons.get((i + 1) % seasons.size()));
            }
        }
        return Optional.empty();
    }

    /**
     * @return how many month days come before that day of that month in the year, Celestine 1
     *         being 0 in SkysongCalendar's calendar
     * @throws IllegalArgumentException when the month isn't one of these, or the day is outside it
     */
    public int dayOfYear(CalendarMonth month, int day) {
        int index = months.indexOf(month);
        if (index < 0) {
            throw new IllegalArgumentException("No month " + month.id() + " in this calendar");
        }
        if (day < 1 || day > monthLength) {
            throw new IllegalArgumentException(month.name().displayName() + " has no day " + day
                    + ": months have days 1 to " + monthLength);
        }
        return index * monthLength + day - 1;
    }

    /** @return {@link #dayOfYear(CalendarMonth, int)} for that day; empty for a festival day or an unknown month */
    public OptionalInt dayOfYear(CalendarDay day) {
        if (day.isFestival()) {
            return OptionalInt.empty();
        }
        String monthId = day.monthId().orElseThrow();
        return months.stream().filter(month -> month.id().equals(monthId)).findFirst()
                .filter(month -> day.day() <= monthLength)
                .map(month -> OptionalInt.of(dayOfYear(month, day.day())))
                .orElse(OptionalInt.empty());
    }

    /**
     * Reads a day of a month written like {@code Rana 10}, matching the month by id or display
     * name, ignoring case.
     *
     * @return the month and the day
     * @throws IllegalArgumentException naming what is wrong, and the months that exist
     */
    public MonthDay monthDay(String text) {
        String written = text == null ? "" : text.strip();
        int space = written.lastIndexOf(' ');
        if (space < 0) {
            throw new IllegalArgumentException("'" + written + "' isn't a date: write a month and a day, like \""
                    + months.getFirst().name().displayName() + " 1\"");
        }
        String monthText = written.substring(0, space);
        CalendarMonth month = month(monthText).orElseThrow(() -> new IllegalArgumentException(
                "no month '" + monthText.strip() + "'; the months are " + monthList()));
        int day;
        try {
            day = Integer.parseInt(written.substring(space + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + written.substring(space + 1) + "' isn't a day number");
        }
        dayOfYear(month, day);
        return new MonthDay(month, day);
    }

    /**
     * A day of a month that comes back every year.
     *
     * @param month the month
     * @param day   the day of the month, from 1
     */
    public record MonthDay(CalendarMonth month, int day) {
        @Override
        public String toString() {
            return month.name().displayName() + " " + day;
        }
    }

    /** @return the seasons' display names, such as {@code Florescia, Su'untah}, for warnings */
    public String seasonList() {
        return seasons.stream().map(CalendarName::displayName).collect(Collectors.joining(", "));
    }

    /** @return the months' display names, for warnings */
    public String monthList() {
        return months.stream().map(month -> month.name().displayName()).collect(Collectors.joining(", "));
    }
}
