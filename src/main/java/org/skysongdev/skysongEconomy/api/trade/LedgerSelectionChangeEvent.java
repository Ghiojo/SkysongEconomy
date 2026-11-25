package org.skysongdev.skysongEconomy.api.trade;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;

import java.util.Objects;

/**
 * A ledger was selected, removed (with its fine paid) or dropped on an Economy and Logistics
 * business. Fired on the main thread, after the change is saved.
 */
public final class LedgerSelectionChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final LedgerSelectionChange change;

    public LedgerSelectionChangeEvent(@NotNull BusinessInfo business, @NotNull LedgerSelectionChange change) {
        this.business = Objects.requireNonNull(business, "business");
        this.change = Objects.requireNonNull(change, "change");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return what changed, and the fine paid */
    public @NotNull LedgerSelectionChange getChange() {
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
