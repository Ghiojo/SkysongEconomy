package org.skysongdev.skysongEconomy.prices.modifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A price with its modifiers, for showing how it was reached.
 *
 * @param base     the expression's value, before modifiers
 * @param applied  the modifiers that changed it, in the order they were set
 * @param skipped  the modifiers that matched but were ignored because the ledger is tax exempt
 * @param percent  the sum of the applied modifiers' percentages
 * @param price    {@code base * (1 + percent / 100)}, truncated, never below 0
 */
public record ModifiedPrice(BigDecimal base, List<PriceModifier> applied, List<PriceModifier> skipped,
                            BigDecimal percent, BigDecimal price) {
    public ModifiedPrice {
        Objects.requireNonNull(base, "base");
        applied = List.copyOf(applied);
        skipped = List.copyOf(skipped);
        Objects.requireNonNull(percent, "percent");
        Objects.requireNonNull(price, "price");
    }

    /** @return whether any modifier applied or was skipped, so there is something to explain */
    public boolean hasModifiers() {
        return !applied.isEmpty() || !skipped.isEmpty();
    }
}
