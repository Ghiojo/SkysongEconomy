package org.skysongdev.skysongEconomy.prices.modifier;

import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * How much a modifier changes a price: a percentage of it, written with {@code %} ({@code +10%}),
 * or a flat amount of money, written as a plain number ({@code +10}). Flat amounts only change
 * Claim prices; trades take percentages.
 */
public sealed interface ModifierAmount {
    /** @return the signed number, such as {@code 10} for {@code +10%} or {@code -5} for {@code -5} */
    BigDecimal value();

    /** @return how storage writes the kind: {@code percent} or {@code flat} */
    String kind();

    /** @return the amount as commands take it, such as {@code +10%} or {@code +250} */
    String signed();

    /** A percentage of the price it changes. */
    record Percent(BigDecimal value) implements ModifierAmount {
        public Percent {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return PERCENT;
        }

        @Override
        public String signed() {
            return PriceModifiers.signed(value) + "%";
        }
    }

    /** A fixed amount of money added to (or taken from) the price it changes. */
    record Flat(BigDecimal value) implements ModifierAmount {
        public Flat {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return FLAT;
        }

        @Override
        public String signed() {
            return PriceModifiers.signed(value);
        }
    }

    /** The {@link #kind()} of a percentage. */
    String PERCENT = "percent";
    /** The {@link #kind()} of a flat amount. */
    String FLAT = "flat";

    /**
     * Reads an amount as commands write it: {@code +10%} or {@code -5%} is a percentage, {@code +10}
     * or {@code 10} a flat amount.
     *
     * @throws PriceException {@code prices.modifiers.invalid-amount} when it isn't a number
     */
    static ModifierAmount parse(String text) {
        String trimmed = text.strip();
        boolean percent = trimmed.endsWith("%");
        String number = percent ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        try {
            BigDecimal value = new BigDecimal(number.startsWith("+") ? number.substring(1) : number);
            return percent ? new Percent(value) : new Flat(value);
        } catch (NumberFormatException e) {
            throw new PriceException("prices.modifiers.invalid-amount", Map.of("amount", text));
        }
    }

    /**
     * Rebuilds an amount from storage.
     *
     * @param kind {@code percent} or {@code flat}; anything else, such as a row from before flat
     *             amounts existed, is read as a percentage
     */
    static ModifierAmount of(String kind, BigDecimal value) {
        return FLAT.equals(kind) ? new Flat(value) : new Percent(value);
    }
}
