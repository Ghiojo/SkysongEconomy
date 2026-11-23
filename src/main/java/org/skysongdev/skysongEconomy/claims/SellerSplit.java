package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * How a resale's asking price is shared between the Claim's owners: equally, each share truncated
 * as prices are, and whatever truncation leaves over goes to the owner who put it on sale (or the
 * first owner, if they no longer own it). The shares always add up to the asking price exactly.
 */
public final class SellerSplit {
    private SellerSplit() {
    }

    /**
     * @param owners   everyone who owns the Claim, at least one
     * @param listedBy who put it on sale
     * @return what each player account receives, in the owners' order; a player with several
     *         owning characters gets their shares summed
     */
    public static Map<UUID, BigDecimal> of(BigDecimal askingPrice, List<CharacterRef> owners, CharacterRef listedBy,
                                           PriceSettings settings) {
        if (owners.isEmpty()) {
            throw new IllegalArgumentException("A resale needs at least one owner");
        }
        BigDecimal each = settings.truncate(askingPrice.divide(BigDecimal.valueOf(owners.size()), MathContext.DECIMAL64));
        Map<UUID, BigDecimal> payouts = new LinkedHashMap<>();
        owners.forEach(owner -> payouts.merge(owner.player(), each, BigDecimal::add));
        BigDecimal remainder = askingPrice.subtract(each.multiply(BigDecimal.valueOf(owners.size())));
        if (remainder.signum() != 0) {
            CharacterRef gets = owners.contains(listedBy) ? listedBy : owners.getFirst();
            payouts.merge(gets.player(), remainder, BigDecimal::add);
        }
        return payouts;
    }
}
