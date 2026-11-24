package org.skysongdev.skysongEconomy.api.claims;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A seller's share of a Claim sale that couldn't be paid when the Claim sold (see
 * {@link ClaimSale#sellerPaidLater()}) was paid into their balance, usually as they joined. Fired on
 * the main thread, once per share.
 */
public final class ClaimPayoutEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID account;
    private final String claimId;
    private final long saleId;
    private final BigDecimal amount;
    private final Instant soldAt;

    public ClaimPayoutEvent(@NotNull UUID account, @NotNull String claimId, long saleId, @NotNull BigDecimal amount,
                            @NotNull Instant soldAt) {
        this.account = Objects.requireNonNull(account, "account");
        this.claimId = Objects.requireNonNull(claimId, "claimId");
        this.saleId = saleId;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.soldAt = Objects.requireNonNull(soldAt, "soldAt");
    }

    /** @return the player paid */
    public @NotNull UUID getAccount() {
        return account;
    }

    /** @return the Claim that was sold */
    public @NotNull String getClaimId() {
        return claimId;
    }

    /** @return the sale it comes from: its {@link ClaimSale#id()} */
    public long getSaleId() {
        return saleId;
    }

    /** @return what they were paid */
    public @NotNull BigDecimal getAmount() {
        return amount;
    }

    /** @return when the Claim sold */
    public @NotNull Instant getSoldAt() {
        return soldAt;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
