package org.skysongdev.skysongEconomy.api.trade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * How a trade's price was reached: its base, the modifiers that changed it, and what was paid.
 * {@code paid = base * (1 + percent / 100)}, truncated to the server's decimals and never below 0,
 * so the modifiers' shares add up to {@code paid - base} up to that truncation.
 *
 * @param base      the price before modifiers
 * @param percent   the sum of the applied modifiers' percentages
 * @param paid      what was actually paid
 * @param modifiers the modifiers that applied, in the order they were set
 * @param taxExempt whether the ledger ignored some modifiers that matched (see {@code exempt-from})
 */
public record TradePrice(BigDecimal base, BigDecimal percent, BigDecimal paid, List<AppliedTradeModifier> modifiers,
                         boolean taxExempt) {
    public TradePrice {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(percent, "percent");
        Objects.requireNonNull(paid, "paid");
        modifiers = List.copyOf(modifiers);
    }

    /** @return a price without modifiers */
    public static TradePrice flat(BigDecimal paid) {
        return new TradePrice(paid, BigDecimal.ZERO, paid, List.of(), false);
    }
}
