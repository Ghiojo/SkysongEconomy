package org.skysongdev.skysongEconomy.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.integration.skysongcalendar.SkysongCalendarAdapter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects SkysongEconomy to SkysongCalendar, which is optional. Without it, or with one too old,
 * the plugin gets {@link CalendarClock#NONE}: nothing that depends on the IC date ever applies,
 * and everything else works as usual.
 * <p>
 * No SkysongCalendar class is loaded until SkysongCalendar is known to be enabled, so the plugin
 * starts without it.
 */
public final class CalendarConnection {
    /** The SkysongCalendar plugin's name, as plugin.yml's {@code softdepend} lists it. */
    public static final String SKYSONG_CALENDAR = "SkysongCalendar";

    private CalendarConnection() {
    }

    /**
     * @return the calendar; {@link CalendarClock#NONE}, with an info line or a console error saying
     *         why, when SkysongCalendar is not installed or can't be used
     */
    public static CalendarClock connect(JavaPlugin plugin) {
        Logger logger = plugin.getLogger();
        Plugin skysongCalendar = plugin.getServer().getPluginManager().getPlugin(SKYSONG_CALENDAR);
        if (skysongCalendar == null || !skysongCalendar.isEnabled()) {
            logger.info(SKYSONG_CALENDAR + " is not installed: nothing changes with the IC date");
            return CalendarClock.NONE;
        }
        try {
            SkysongCalendarAdapter adapter = SkysongCalendarAdapter.connect();
            logger.info("Connected to " + SKYSONG_CALENDAR + " (API " + adapter.apiVersion() + ")");
            return adapter;
        } catch (RuntimeException | LinkageError e) {
            logger.log(Level.SEVERE, "Nothing changes with the IC date: " + SKYSONG_CALENDAR + " could not be used", e);
            return CalendarClock.NONE;
        }
    }
}
