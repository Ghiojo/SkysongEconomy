package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * What buying a Claim costs right now, and how that was reached: the asking price, which goes to
 * the seller, plus each modifier's share, which never does.
 *
 * @param claimId       the Claim
 * @param claimTypeId   its SkysongClaims type
 * @param askingPrice   what the seller asks, truncated
 * @param shares        each applying modifier with what it adds, in the order the modifiers were set
 * @param modifierTotal the sum of the shares; never negative, since Claim modifiers only raise prices
 * @param total         what the buyer pays: {@code askingPrice + modifierTotal}
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
     * What one modifier adds to a Claim's price.
     *
     * @param modifier the modifier
     * @param amount   its share: the asking price times its percentage, or its flat amount, truncated
     */
    public record Share(PriceModifier modifier, BigDecimal amount) {
        public Share {
            Objects.requireNonNull(modifier, "modifier");
            Objects.requireNonNull(amount, "amount");
        }
    }

    /** @return whether any modifier raises the price */
    public boolean isModified() {
        return !shares.isEmpty();
    }
}
