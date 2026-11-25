package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Someone paid for a training session at a business and started it. Fired on the main thread. The
 * price went from their balance to the business's vault.
 */
public final class TrainingStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final TrainingInfo session;

    public TrainingStartEvent(@NotNull BusinessInfo business, @NotNull TrainingInfo session) {
        this.business = Objects.requireNonNull(business, "business");
        this.session = Objects.requireNonNull(session, "session");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the session: who, when, how long, and what was paid */
    public @NotNull TrainingInfo getSession() {
        return session;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
