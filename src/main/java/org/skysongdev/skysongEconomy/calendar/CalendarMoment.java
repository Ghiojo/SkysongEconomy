package org.skysongdev.skysongEconomy.calendar;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A time of the IC year that staff ask about, such as {@code /se node odds mine 2 Rana 10}: one
 * day, or a whole season. It is sealed, so a {@code switch} over it covers every case.
 */
public sealed interface CalendarMoment {
    /** What staff type for the festival days that close the year. */
    String FESTIVAL = "festival";
    /** What staff type for today. */
    String TODAY = "today";

    /** One day: a day of a month, or a festival day. */
    record Day(CalendarDay day) implements CalendarMoment {
        public Day {
            Objects.requireNonNull(day, "day");
        }
    }

    /** A whole season, by id. */
    record Season(String seasonId) implements CalendarMoment {
        public Season {
            Objects.requireNonNull(seasonId, "seasonId");
        }
    }

    /**
     * Reads what staff typed: {@code today}, a season ({@code Vinse'er}), a day of a month
     * ({@code Rana 10}) or {@code festival} (its first day).
     *
     * @param today today's IC day, for {@code today}
     * @throws IllegalArgumentException naming what isn't understood, and the seasons and months
     */
    static CalendarMoment parse(String text, CalendarNames names, Optional<CalendarDay> today) {
        String written = text.strip();
        String lower = written.toLowerCase(Locale.ROOT);
        if (lower.equals(TODAY)) {
            return new Day(today.orElseThrow(() -> new IllegalArgumentException("today's IC date isn't known")));
        }
        if (lower.equals(FESTIVAL)) {
            return new Day(CalendarDay.festival(names.festivalSeasonId(), 1));
        }
        Optional<CalendarName> season = names.season(written);
        if (season.isPresent()) {
            return new Season(season.get().id());
        }
        try {
            CalendarNames.MonthDay day = names.monthDay(written);
            return new Day(CalendarDay.of(day.month().seasonId(), day.month().id(), day.day()));
        } catch (IllegalArgumentException notADay) {
            throw new IllegalArgumentException("'" + written + "' isn't today, " + FESTIVAL + ", a season ("
                    + names.seasonList() + ") or a day like \"" + names.months().getFirst().name().displayName()
                    + " 1\"");
        }
    }
}
