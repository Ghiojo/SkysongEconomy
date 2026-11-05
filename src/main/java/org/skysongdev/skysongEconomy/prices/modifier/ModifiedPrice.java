package org.skysongdev.skysongEconomy.prices.modifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A price with its modifiers, for showing how it was reached.
 *
 * @param base     the expression's value, before modifiers
 * @param applied  the modifiers that changed it, in the order they were set
 * @param percent  the sum of their percentages
 * @param price    {@code base * (1 + percent / 100)}, truncated, never below 0
 */
public record ModifiedPrice(BigDecimal base, List<PriceModifier> applied, BigDecimal percent, BigDecimal price) {
    public ModifiedPrice {
        Objects.requireNonNull(base, "base");
        applied = List.copyOf(applied);
        Objects.requireNonNull(percent, "percent");
        Objects.requireNonNull(price, "price");
    }
}
