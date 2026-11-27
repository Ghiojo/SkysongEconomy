package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * What buying a Claim costs right now, and how that was reached: the asking price, which goes to
 * the seller, plus each modifier's share, which never does. A discount's share is negative; the
 * seller still receives the whole asking price.
 *
 * @param claimId       the Claim
 * @param claimTypeId   its SkysongClaims type
 * @param askingPrice   what the seller asks, truncated
 * @param shares        each applying modifier with what it adds (negative for a discount), in the order the
 *                      modifiers were set
 * @param modifierTotal the sum of the shares, never below {@code -askingPrice}
 * @param total         what the buyer pays: {@code askingPrice + modifierTotal}, never below 0
 */
public record ClaimQuote(String claimId, String claimTypeId, BigDecimal askingPrice, List<Share> shares,
                         BigDecimal modifierTotal, BigDecimal total) {
    public ClaimQuote {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(claimTypeId, "claimTypeId");
        Objects.requireNonNull(askingPrice, "askingPrice");
        shares = List.copyOf(shares);
        Objects.requireNonNull(modifierTotal, "modifierTotal");
        Objects.requireNonNull(total, "total");
    }

    /**
     * What one modifier adds to a Claim's price, or takes off it.
     *
     * @param modifier the modifier
     * @param amount   its share: the asking price times its percentage (negative for a discount), or its flat
     *                 amount, truncated
     */
    public record Share(PriceModifier modifier, BigDecimal amount) {
        public Share {
            Objects.requireNonNull(modifier, "modifier");
            Objects.requireNonNull(amount, "amount");
        }

        /** @return {@code -} for a discount, otherwise {@code +}: shown before {@link #magnitude()} */
        public String sign() {
            return amount.signum() < 0 ? "-" : "+";
        }

        /** @return the share without its sign */
        public BigDecimal magnitude() {
            return amount.abs();
        }
    }

    /** @return whether any modifier applies, even when they cancel out */
    public boolean isModified() {
        return !shares.isEmpty();
    }

    /** @return whether the buyer pays more, less or the same as the asking price */
    public Change change() {
        return switch (modifierTotal.signum()) {
            case 1 -> Change.RAISED;
            case -1 -> Change.LOWERED;
            default -> Change.NONE;
        };
    }

    /** How the modifiers change what the buyer pays, compared with the asking price. */
    public enum Change {
        /** The buyer pays the asking price. */
        NONE,
        /** The buyer pays more. */
        RAISED,
        /** The buyer pays less (discounts); the seller still gets the asking price. */
        LOWERED
    }
}
