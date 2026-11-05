package org.skysongdev.skysongEconomy.prices.modifier;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A percentage change to the prices of a target, such as a port tax of +10% on one ledger.
 * A target holds at most one modifier of each type; modifiers of different types add up.
 *
 * @param id          generated, used by commands to change or remove it
 * @param type        a key such as {@code port_tax}, so mechanics can find and replace their own modifiers
 * @param target      what it is attached to
 * @param appliesTo   imports, exports or both
 * @param percent     signed, e.g. {@code 10} or {@code -5}
 * @param displayName MiniMessage text shown when hovering a price, e.g. {@code Port tax +10%}
 * @param expiresAt   when it stops applying, or {@code null} for never
 * @param createdBy   who set it, e.g. a player name or {@code CONSOLE}
 * @param createdAt   when it was set
 */
public record PriceModifier(String id, String type, ModifierTarget target, AppliesTo appliesTo, BigDecimal percent,
                            String displayName, @Nullable Instant expiresAt, String createdBy, Instant createdAt) {
    public PriceModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(appliesTo, "appliesTo");
        Objects.requireNonNull(percent, "percent");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public Optional<Instant> expiry() {
        return Optional.ofNullable(expiresAt);
    }

    /** @return whether it still applies at that time */
    public boolean isActive(Instant now) {
        return expiresAt == null || now.isBefore(expiresAt);
    }

    /** @return whether it changes the price of that subject */
    public boolean appliesTo(PriceSubject subject) {
        return appliesTo.covers(subject.direction()) && target.matches(subject);
    }
}
