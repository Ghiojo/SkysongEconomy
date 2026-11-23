package org.skysongdev.skysongEconomy.calendar;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    /** @return the seasons' display names, such as {@code Florescia, Su'untah}, for warnings */
    public String seasonList() {
        return seasons.stream().map(CalendarName::displayName).collect(Collectors.joining(", "));
    }

    /** @return the months' display names, for warnings */
    public String monthList() {
        return months.stream().map(month -> month.name().displayName()).collect(Collectors.joining(", "));
    }
}
