package org.skysongdev.skysongEconomy.api.prices;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A price modifier was added, replaced, removed or expired. Fired on the main thread, after the
 * change is in memory. An expiry is noticed (and fired) the next time modifiers are read, such as
 * when a price is shown, so it can come a little after the expiry time.
 */
public final class PriceModifierChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final PriceModifierChange change;

    public PriceModifierChangeEvent(@NotNull PriceModifierChange change) {
        this.change = Objects.requireNonNull(change, "change");
    }

    /** @return what changed */
    public @NotNull PriceModifierChange getChange() {
        return change;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
