package org.skysongdev.skysongEconomy.integration.skysongcalendar;

import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongCalendar.api.Festival;
import org.skysongdev.skysongCalendar.api.IcDate;
import org.skysongdev.skysongCalendar.api.Month;
import org.skysongdev.skysongCalendar.api.Season;
import org.skysongdev.skysongCalendar.api.SkysongCalendarApi;
import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.CalendarMonth;
import org.skysongdev.skysongEconomy.calendar.CalendarName;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.integration.ApiVersion;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The only class that calls the SkysongCalendar API. It turns SkysongCalendar's dates, seasons and
 * months into the plugin's own {@link CalendarDay} and {@link CalendarNames}, so nothing else
 * depends on SkysongCalendar. The calendar API is safe from any thread, and so is this.
 */
public final class SkysongCalendarAdapter implements CalendarClock {
    /** The oldest SkysongCalendar API this plugin can use. */
    public static final ApiVersion REQUIRED_API = new ApiVersion(1, 0, 0);

    private final SkysongCalendarApi api;
    private final Plugin plugin;
    private final CalendarNames names;

    private SkysongCalendarAdapter(SkysongCalendarApi api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
        this.names = new CalendarNames(
                Arrays.stream(Season.values()).map(SkysongCalendarAdapter::name).toList(),
                Arrays.stream(Month.values())
                        .map(month -> new CalendarMonth(name(month), CalendarName.idOf(month.season().name())))
                        .toList(),
                Month.LENGTH,
                CalendarName.idOf(Festival.FESTIVAL.season().name()));
    }

    /**
     * Connects to the SkysongCalendar API that the running SkysongCalendar registered.
     *
     * @param plugin SkysongEconomy, which listens to SkysongCalendar's events
     * @throws IllegalStateException when SkysongCalendar is not enabled, or its API is older than {@link #REQUIRED_API}
     */
    public static SkysongCalendarAdapter connect(Plugin plugin) {
        SkysongCalendarApi api = SkysongCalendarApi.get();
        if (!ApiVersion.parse(api.apiVersion()).isAtLeast(REQUIRED_API)) {
            throw new IllegalStateException("SkysongCalendar has API " + api.apiVersion() + ", but API " + REQUIRED_API
                    + " or newer is needed: update SkysongCalendar");
        }
        return new SkysongCalendarAdapter(api, Objects.requireNonNull(plugin, "plugin"));
    }

    /** @return the API version the installed SkysongCalendar implements */
    public String apiVersion() {
        return api.apiVersion();
    }

    @Override
    public Optional<CalendarDay> today() {
        return Optional.of(day(api.calendar().today()));
    }

    @Override
    public Optional<CalendarNames> names() {
        return Optional.of(names);
    }

    @Override
    public void whenDayChanges(Runnable action) {
        Objects.requireNonNull(action, "action");
        plugin.getServer().getPluginManager().registerEvents(new CalendarDayListener(action), plugin);
    }

    /** @return the date as the plugin's rules see it */
    static CalendarDay day(IcDate date) {
        String season = CalendarName.idOf(date.season().name());
        return switch (date.period()) {
            case Month month -> CalendarDay.of(season, CalendarName.idOf(month.name()), date.day());
            case Festival festival -> CalendarDay.festival(season, date.day());
        };
    }

    private static CalendarName name(Season season) {
        return new CalendarName(CalendarName.idOf(season.name()), season.displayName());
    }

    private static CalendarName name(Month month) {
        return new CalendarName(CalendarName.idOf(month.name()), month.displayName());
    }
}
