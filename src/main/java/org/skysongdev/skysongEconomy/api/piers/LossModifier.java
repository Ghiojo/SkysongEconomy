package org.skysongdev.skysongEconomy.api.piers;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Raises or lowers a pier's loss chance: the chance that a shipment collected there is lost.
 * <p>
 * A flat modifier ({@code +5}) adds percentage points; a percentage one ({@code +10%}) raises the
 * chance by that share of itself. With a {@link #tag()}, it only counts for shipments carrying
 * that tag, so {@code new LossModifier("guards", BigDecimal.valueOf(5), false, Optional.of("contraband"))}
 * makes a pier 5 points likelier to catch contraband and leaves honest cargo alone.
 * <p>
 * A pier holds one modifier per {@link #id()}: setting another with the same id replaces it, and
 * different ids stack.
 *
 * @param id      what the modifier is, such as {@code guards}; letters, digits, {@code _} and {@code -}
 * @param amount  signed: positive raises the chance, negative lowers it
 * @param percent whether {@link #amount()} is a percentage of the chance rather than points
 * @param tag     the shipment tag it is limited to; empty for every shipment
 */
public record LossModifier(String id, BigDecimal amount, boolean percent, Optional<String> tag) {
    public LossModifier {
        id = Piers.checkId(id, "loss modifier id");
        Objects.requireNonNull(amount, "amount");
        tag = Objects.requireNonNull(tag, "tag").map(text -> text.strip().toLowerCase(Locale.ROOT))
                .filter(text -> !text.isEmpty());
    }

    /** A modifier for every shipment. */
    public LossModifier(String id, BigDecimal amount, boolean percent) {
        this(id, amount, percent, Optional.empty());
    }

    /**
     * Reads an amount as config and commands write it: {@code +5} or {@code -5} are points,
     * {@code +10%} a percentage.
     *
     * @throws NumberFormatException when it isn't a number
     */
    public static LossModifier parse(String id, String amount, Optional<String> tag) {
        String trimmed = amount.strip();
        boolean percent = trimmed.endsWith("%");
        String number = percent ? trimmed.substring(0, trimmed.length() - 1).strip() : trimmed;
        return new LossModifier(id, new BigDecimal(number.startsWith("+") ? number.substring(1) : number), percent, tag);
    }

    /** @return whether it counts for a shipment with those tags */
    public boolean appliesTo(Set<String> shipmentTags) {
        return tag.isEmpty() || shipmentTags.contains(tag.get());
    }

    /** @return the amount as config writes it, such as {@code +5} or {@code -10%} */
    public String signed() {
        String plain = amount.stripTrailingZeros().toPlainString();
        return (amount.signum() > 0 ? "+" + plain : plain) + (percent ? "%" : "");
    }
}
