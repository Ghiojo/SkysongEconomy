package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A business was created, deleted or changed, and the change is saved. Fired on the main thread,
 * once per change: a tier change that cost workers fires {@link BusinessChange.Kind#TIER_CHANGED}
 * and then one {@link BusinessChange.Kind#WORKER_FIRED} per worker. It can't be cancelled.
 */
public final class BusinessChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessChange change;

    public BusinessChangeEvent(@NotNull BusinessChange change) {
        this.change = Objects.requireNonNull(change, "change");
    }

    /** @return what changed, with the business before and after */
    public @NotNull BusinessChange getChange() {
        return change;
    }

    /** @return what changed */
    public @NotNull BusinessChange.Kind getKind() {
        return change.kind();
    }

    /** @return the business's id */
    public @NotNull String getBusinessId() {
        return change.businessId();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
