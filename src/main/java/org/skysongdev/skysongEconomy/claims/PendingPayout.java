package org.skysongdev.skysongEconomy.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A seller's share of a Claim sale that couldn't be paid when it sold (for example the coins had no
 * room while they were offline). It is paid when they next join.
 *
 * @param saleId  the sale it comes from
 * @param account the player owed
 * @param claimId the Claim sold
 * @param amount  what they are owed
 * @param soldAt  when the Claim sold
 */
public record PendingPayout(long saleId, UUID account, String claimId, BigDecimal amount, Instant soldAt) {
    public PendingPayout {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(soldAt, "soldAt");
    }
}
