package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A shift has ended: its vault cut is in the business's vault and its pay can be retrieved.
 * Fired on the main thread. The player may be offline by now.
 */
public final class ShiftEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final ShiftInfo shift;

    public ShiftEndEvent(@NotNull BusinessInfo business, @NotNull ShiftInfo shift) {
        this.business = Objects.requireNonNull(business, "business");
        this.shift = Objects.requireNonNull(shift, "shift");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the shift: who worked it, for how long, its pay and vault cut */
    public @NotNull ShiftInfo getShift() {
        return shift;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
