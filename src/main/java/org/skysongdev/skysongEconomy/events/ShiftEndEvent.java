package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.shift.Shift;

/** A shift has ended and its pay can be retrieved. The player may be offline by now. */
public final class ShiftEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Business business;
    private final Shift shift;

    public ShiftEndEvent(Business business, Shift shift) {
        this.business = business;
        this.shift = shift;
    }

    public Business getBusiness() {
        return business;
    }

    /** @return the shift, with who worked it and what it pays */
    public Shift getShift() {
        return shift;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
