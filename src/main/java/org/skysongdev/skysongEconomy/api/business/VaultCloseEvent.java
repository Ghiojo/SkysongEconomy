package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A deleted business's vault was emptied: paid to its owner or, for a server-owned business, left
 * the economy. Fired on the main thread, only when the vault held money.
 */
public final class VaultCloseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final @Nullable UUID payee;
    private final BigDecimal amount;
    private final boolean paid;

    public VaultCloseEvent(@NotNull BusinessInfo business, @Nullable UUID payee, @NotNull BigDecimal amount,
                           boolean paid) {
        this.business = Objects.requireNonNull(business, "business");
        this.payee = payee;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.paid = paid;
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the owner's player who was paid; empty for a server-owned business, whose vault left the economy */
    public @NotNull Optional<UUID> getPayee() {
        return Optional.ofNullable(payee);
    }

    /** @return what the vault held */
    public @NotNull BigDecimal getAmount() {
        return amount;
    }

    /** @return whether the owner was paid; {@code false} when the payment failed (SkysongEconomy logs a warning) or there was nobody to pay */
    public boolean isPaid() {
        return paid;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
