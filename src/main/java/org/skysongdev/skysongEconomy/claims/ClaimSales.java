package org.skysongdev.skysongEconomy.claims;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The rules for pricing Claims and putting them on sale. Every change to a Claim's price or sale
 * goes through here, so the checks are in one place. Main thread only.
 * <ul>
 *     <li>Free Claims are never priced or sold.</li>
 *     <li>The server only sells Claims nobody owns, and only with a price.</li>
 *     <li>A Claim another plugin put on sale is left alone.</li>
 *     <li>When the last owner of a priced Claim leaves, it goes back on sale at its price.</li>
 * </ul>
 */
public final class ClaimSales {
    /** The plugin name SkysongClaims records for what SkysongEconomy puts on sale. */
    public static final String SELLER = "SkysongEconomy";

    private final ClaimGateway claims;
    private final ClaimPrices prices;

    public ClaimSales(ClaimGateway claims, ClaimPrices prices) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.prices = Objects.requireNonNull(prices, "prices");
    }

    /** @return the asking prices */
    public ClaimPrices prices() {
        return prices;
    }

    /**
     * Sets a Claim's asking price. A Claim on sale stays on sale at the new price.
     *
     * @return what the price is worth now
     * @throws ClaimException when there is no such Claim or it is a Free Claim
     * @throws PriceException when the price can't be used
     */
    public BigDecimal setPrice(String claimId, String text, String by) {
        ClaimView claim = require(claimId);
        refuseFree(claim);
        return prices.set(claim.id(), text, by);
    }

    /**
     * Removes a Claim's asking price. A Claim the server had on sale is taken off sale, since it
     * can't be sold without one.
     *
     * @return whether it was on sale
     * @throws ClaimException {@code claims.unknown-claim}, or {@code claims.no-price} when it has no price
     */
    public boolean clearPrice(String claimId) {
        String id = claims.find(claimId).map(ClaimView::id).orElse(claimId);
        if (!prices.remove(id)) {
            throw ClaimException.about("claims.no-price", id);
        }
        return claims.find(id).filter(this::isOurs).map(claim -> {
            claims.setForSale(claim.id(), false);
            return true;
        }).orElse(false);
    }

    /**
     * Puts an unowned Claim on sale for the server, at the price given or its saved one.
     *
     * @param text a new asking price, or {@code null} to keep the saved one
     * @return what it sells for now, before modifiers
     * @throws ClaimException when there is no such Claim, it is free, has owners, is for sale by
     *                        another plugin, or has no price
     * @throws PriceException when the new price can't be used
     */
    public BigDecimal sell(String claimId, @Nullable String text, String by) {
        ClaimView claim = require(claimId);
        refuseFree(claim);
        if (claim.isOwned()) {
            throw ClaimException.about("claims.owned", claim.id());
        }
        if (claim.isForSale() && !isOurs(claim)) {
            throw new ClaimException("claims.sold-elsewhere", Map.of("claim", claim.id(), "plugin",
                    claim.forSaleBy().orElseThrow()));
        }
        BigDecimal value;
        if (text != null) {
            value = prices.set(claim.id(), text, by);
        } else {
            value = prices.value(claim.id()).orElseThrow(() -> ClaimException.about("claims.no-price", claim.id()));
        }
        if (!isOurs(claim)) {
            claims.setForSale(claim.id(), true);
        }
        return value;
    }

    /**
     * Takes a Claim the server put on sale off sale. Its price stays saved.
     *
     * @throws ClaimException {@code claims.unknown-claim}, or {@code claims.not-for-sale} when SkysongEconomy isn't selling it
     */
    public void unsell(String claimId) {
        ClaimView claim = require(claimId);
        if (!isOurs(claim)) {
            throw ClaimException.about("claims.not-for-sale", claim.id());
        }
        claims.setForSale(claim.id(), false);
    }

    /** @return every Claim SkysongEconomy has on sale */
    public List<ClaimView> listed() {
        return claims.all().stream().filter(this::isOurs).toList();
    }

    /**
     * A Claim lost its last owner. If it has a price and isn't free, it goes back on sale at that price.
     *
     * @return whether it is on sale again
     */
    public boolean abandoned(String claimId) {
        return claims.find(claimId)
                .filter(claim -> !claim.free() && !claim.isOwned() && !claim.isForSale())
                .filter(claim -> prices.find(claim.id()).isPresent())
                .map(claim -> {
                    claims.setForSale(claim.id(), true);
                    return true;
                }).orElse(false);
    }

    /** A Claim was deleted in SkysongClaims: its price goes too. */
    public void deleted(String claimId) {
        prices.remove(claimId);
    }

    /**
     * Brings saved prices in line with SkysongClaims after a start, since Claims may have changed
     * while SkysongEconomy was off: prices of Claims that no longer exist, or are now free, are
     * removed, and Claims the server has on sale without a price are taken off sale.
     *
     * @param changes told about each change made
     */
    public void reconcile(Consumer<String> changes) {
        for (String id : new ArrayList<>(prices.all().keySet())) {
            Optional<ClaimView> claim = claims.find(id);
            if (claim.isEmpty() || claim.get().free()) {
                prices.remove(id);
                changes.accept("Removed the price of " + id + ": "
                        + (claim.isEmpty() ? "SkysongClaims no longer has it" : "it is a Free Claim now"));
            }
        }
        for (ClaimView claim : listed()) {
            if (prices.find(claim.id()).isEmpty() || claim.free()) {
                claims.setForSale(claim.id(), false);
                changes.accept("Took " + claim.id() + " off sale: it has no price");
            }
        }
    }

    /** @return whether SkysongEconomy put the Claim on sale */
    public boolean isOurs(ClaimView claim) {
        return claim.isForSaleBy(SELLER);
    }

    private ClaimView require(String claimId) {
        return claims.find(claimId).orElseThrow(() -> ClaimException.about("claims.unknown-claim", claimId));
    }

    private static void refuseFree(ClaimView claim) {
        if (claim.free()) {
            throw ClaimException.about("claims.free", claim.id());
        }
    }
}
