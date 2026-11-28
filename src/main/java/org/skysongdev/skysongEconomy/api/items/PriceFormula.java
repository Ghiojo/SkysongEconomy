package org.skysongdev.skysongEconomy.api.items;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * A parsed price formula from {@link PriceFormulas#parse}. Values are worked out from the price
 * database on every call, so they follow {@code /se ledger} changes at once. Immutable; call
 * {@link #value} on the main thread.
 */
public interface PriceFormula {
    /** @return the formula as written, without surrounding spaces */
    String text();

    /** @return the lower-case material ids its {@code {item:...}} references name, in order */
    Set<String> items();

    /** @return whether it uses {@code {base}} */
    boolean usesBase();

    /**
     * Works the formula out without a {@code {base}}.
     *
     * @return the value truncated to the decimals of {@code prices.yml}, as players see and pay
     *         it, never below 0; empty when it uses {@code {base}}, names an item without a base
     *         value, or divides by zero
     */
    Optional<BigDecimal> value();

    /**
     * Works the formula out with {@code {base}} standing for the given value.
     *
     * @param base what {@code {base}} means where the caller uses the formula
     * @return the value truncated to the decimals of {@code prices.yml}, never below 0; empty when
     *         it names an item without a base value, or divides by zero
     */
    Optional<BigDecimal> value(BigDecimal base);
}
