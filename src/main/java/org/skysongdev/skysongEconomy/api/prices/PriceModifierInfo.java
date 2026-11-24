package org.skysongdev.skysongEconomy.api.prices;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A price modifier as other plugins see it, such as a port tax of +10% on one ledger or a registry
 * fee of +50 on every house. It is a copy: it doesn't change when the modifier does.
 *
 * @param id          generated, as {@code /se modifier list} shows it, such as {@code m12}
 * @param type        a key such as {@code port_tax}; a target holds at most one modifier of each type
 * @param target      what it is set on, such as {@code global}, {@code ledger:smugglers} or {@code pier:north_dock}
 * @param appliesTo   which prices it changes
 * @param amount      the signed number, such as 10 for {@code +10%} or -5 for {@code -5%}
 * @param flat        whether {@link #amount} is money added to the price (Claims only) instead of a percentage
 * @param displayName MiniMessage text players see when hovering a price
 * @param expiresAt   when it stops applying; empty for never
 * @param createdBy   who set it, such as a player name or {@code CONSOLE}
 * @param createdAt   when it was set
 */
public record PriceModifierInfo(String id, String type, String target, AppliesTo appliesTo, BigDecimal amount,
                                boolean flat, String displayName, Optional<Instant> expiresAt, String createdBy,
                                Instant createdAt) {
    public PriceModifierInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(appliesTo, "appliesTo");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Which prices a modifier changes. New constants may be added. */
    public enum AppliesTo {
        /** Import deliveries. */
        IMPORTS,
        /** Export sales. */
        EXPORTS,
        /** Both imports and exports. */
        BOTH,
        /** Claim (house) prices. */
        CLAIMS
    }
}
