package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A business's vault was collected: everything in it went to its owner or, for a server-owned
 * business, to the staff member collecting. Fired on the main thread.
 */
public final class VaultCollectEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final UUID payee;
    private final BigDecimal amount;

    public VaultCollectEvent(@NotNull BusinessInfo business, @NotNull UUID payee, @NotNull BigDecimal amount) {
        this.business = Objects.requireNonNull(business, "business");
        this.payee = Objects.requireNonNull(payee, "payee");
        this.amount = Objects.requireNonNull(amount, "amount");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the player paid */
    public @NotNull UUID getPayee() {
        return payee;
    }

    /** @return what was paid */
    public @NotNull BigDecimal getAmount() {
        return amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
