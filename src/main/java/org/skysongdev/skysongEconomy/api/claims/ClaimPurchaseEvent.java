package org.skysongdev.skysongEconomy.api.claims;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A Claim was bought. Fired on the main thread right after the money moved, the buyer became the
 * owner, and the sale was recorded. It can't be cancelled: the sale is done. A plugin that wants to
 * stop some sales does it before, with permissions or Claim modifiers.
 */
public final class ClaimPurchaseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ClaimSale sale;

    public ClaimPurchaseEvent(@NotNull ClaimSale sale) {
        this.sale = Objects.requireNonNull(sale, "sale");
    }

    /** @return everything about the sale: who bought what, the asking price, and each modifier's share */
    public @NotNull ClaimSale getSale() {
        return sale;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
