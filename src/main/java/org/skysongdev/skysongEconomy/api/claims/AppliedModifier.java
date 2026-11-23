package org.skysongdev.skysongEconomy.api.claims;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One modifier that raised a Claim's price at the moment it was bought, and how much of the price
 * it was. Its share was worked out from the asking price alone, so shares never depend on each other.
 *
 * @param id          the modifier's id, as {@code /se modifier list} shows it, such as {@code m12}
 * @param key         its type key, such as {@code district_tax}; the same key on several Claim types
 *                    is the same mechanic
 * @param target      what it was set on, such as {@code claim-type:house}, {@code claim:rose_house} or {@code global}
 * @param kind        {@code percent} or {@code flat}
 * @param value       the percentage (10 for +10%) or the flat amount
 * @param share       what it added to the price, in money; never negative
 * @param displayName the name players saw for it
 */
public record AppliedModifier(String id, String key, String target, String kind, BigDecimal value, BigDecimal share,
                              String displayName) {
    public AppliedModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(share, "share");
        Objects.requireNonNull(displayName, "displayName");
    }
}
