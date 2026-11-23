package org.skysongdev.skysongEconomy.integration.skysongcalendar;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongCalendar.api.event.CalendarReloadEvent;
import org.skysongdev.skysongCalendar.api.event.IcDayChangeEvent;

import java.util.Objects;

/** Runs an action when SkysongCalendar starts a new IC day or reloads, after every other listener. */
public final class CalendarDayListener implements Listener {
    private final Runnable action;

    public CalendarDayListener(Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNewDay(IcDayChangeEvent event) {
        action.run();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReload(CalendarReloadEvent event) {
        action.run();
    }
}
