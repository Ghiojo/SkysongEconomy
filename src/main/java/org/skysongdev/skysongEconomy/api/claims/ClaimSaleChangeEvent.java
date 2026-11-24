package org.skysongdev.skysongEconomy.api.claims;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A Claim's asking price changed, or SkysongEconomy put it on sale or took it off sale. Fired on the
 * main thread, after the change. Changes made when the plugin starts, to match what changed in
 * SkysongClaims while it was off, fire too.
 */
public final class ClaimSaleChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ClaimSaleChange change;

    public ClaimSaleChangeEvent(@NotNull ClaimSaleChange change) {
        this.change = Objects.requireNonNull(change, "change");
    }

    /** @return what changed */
    public @NotNull ClaimSaleChange getChange() {
        return change;
    }

    /** @return what happened */
    public @NotNull ClaimSaleChange.Kind getKind() {
        return change.kind();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
