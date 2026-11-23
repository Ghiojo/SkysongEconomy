package org.skysongdev.skysongEconomy.claims;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The rules for pricing Claims and putting them on sale. Every change to a Claim's price or sale
 * goes through here, so the checks are in one place. Main thread only.
 * <ul>
 *     <li>Free Claims are never priced or sold.</li>
 *     <li>The server only sells Claims nobody owns, and only with a price.</li>
 *     <li>An owner can put their whole Claim on sale (an owner listing); the price they ask becomes
 *         the Claim's price. Any owner can take it off sale.</li>
 *     <li>A Claim another plugin put on sale is left alone.</li>
 *     <li>When the last owner of a priced Claim leaves, it goes back on sale at its price, for the
 *         server; an owner listing whose owners all left becomes a server sale.</li>
 * </ul>
 */
public final class ClaimSales {
    /** The plugin name SkysongClaims records for what SkysongEconomy puts on sale. */
    public static final String SELLER = "SkysongEconomy";

    private final ClaimGateway claims;
    private final ClaimPrices prices;
    private final ClaimListings listings;
    private final List<Consumer<String>> listeners = new ArrayList<>();
    /** Claims changing hands right now, whose owners briefly being none is not an abandonment. */
    private final Set<String> transferring = new HashSet<>();

    public ClaimSales(ClaimGateway claims, ClaimPrices prices, ClaimListings listings) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.prices = Objects.requireNonNull(prices, "prices");
        this.listings = Objects.requireNonNull(listings, "listings");
    }

    /**
     * Tells the listener the id of every Claim whose price or sale changed here, after the change,
     * so what shows a price (such as a Claim sign) can show the new one.
     */
    public void addListener(Consumer<String> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** @return the asking prices */
    public ClaimPrices prices() {
        return prices;
    }

    /** @return the Claims on sale for their owners rather than for the server */
    public ClaimListings listings() {
        return listings;
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
        BigDecimal value = prices.set(claim.id(), text, by);
        changed(claim.id());
        return value;
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
        boolean wasListed = claims.find(id).filter(this::isOurs).map(claim -> {
            claims.setForSale(claim.id(), false);
            return true;
        }).orElse(false);
        listings.remove(id);
        changed(id);
        return wasListed;
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
        changed(claim.id());
        return value;
    }

    /**
     * An owner puts the whole Claim on sale. The price they ask becomes the Claim's price, and it
     * stays on sale until it is bought or any owner takes it off sale. Listing it again changes the price.
     *
     * @return what it sells for now, before modifiers
     * @throws ClaimException when there is no such Claim, it is free, the character doesn't own it,
     *                        or another plugin is selling it
     * @throws PriceException when the price can't be used
     */
    public BigDecimal listByOwner(CharacterRef owner, String claimId, String text) {
        ClaimView claim = require(claimId);
        refuseFree(claim);
        refuseNonOwner(claim, owner);
        if (claim.isForSale() && !isOurs(claim)) {
            throw new ClaimException("claims.sold-elsewhere", Map.of("claim", claim.id(), "plugin",
                    claim.forSaleBy().orElseThrow()));
        }
        BigDecimal value = prices.set(claim.id(), text, owner.profile());
        listings.put(claim.id(), owner);
        if (!isOurs(claim)) {
            claims.setForSale(claim.id(), true);
        }
        changed(claim.id());
        return value;
    }

    /**
     * An owner takes their Claim off sale, whichever owner put it on sale. Its price stays saved.
     *
     * @throws ClaimException when there is no such Claim, the character doesn't own it, or SkysongEconomy isn't selling it
     */
    public void unsellByOwner(CharacterRef owner, String claimId) {
        ClaimView claim = require(claimId);
        refuseNonOwner(claim, owner);
        unsell(claim.id());
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
        listings.remove(claim.id());
        changed(claim.id());
    }

    /**
     * A Claim SkysongEconomy had on sale was bought: it comes off sale. Its price stays saved, so it
     * goes back on sale at that price if its owners ever leave.
     */
    public void sold(String claimId) {
        claims.find(claimId).filter(this::isOurs).ifPresent(claim -> claims.setForSale(claim.id(), false));
        listings.remove(claimId);
        changed(claimId);
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
        if (transferring.contains(claimId)) {
            return false;
        }
        if (claims.find(claimId).filter(claim -> !claim.isOwned()).isPresent() && listings.remove(claimId)) {
            // Its sellers are gone: whoever buys it now buys it from the server.
            changed(claimId);
            return false;
        }
        return claims.find(claimId)
                .filter(claim -> !claim.free() && !claim.isOwned() && !claim.isForSale())
                .filter(claim -> prices.find(claim.id()).isPresent())
                .map(claim -> {
                    claims.setForSale(claim.id(), true);
                    changed(claim.id());
                    return true;
                }).orElse(false);
    }

    /** A Claim was deleted in SkysongClaims: its price goes too. */
    public void deleted(String claimId) {
        prices.remove(claimId);
        listings.remove(claimId);
    }

    /**
     * Runs the steps that change the Claim's owners for a sale. Meanwhile the Claim having no owners
     * doesn't put it back on sale.
     *
     * @return what the steps return
     */
    public <T> T transfer(String claimId, Supplier<T> steps) {
        transferring.add(claimId);
        try {
            return steps.get();
        } finally {
            transferring.remove(claimId);
        }
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
                changed(id);
                changes.accept("Removed the price of " + id + ": "
                        + (claim.isEmpty() ? "SkysongClaims no longer has it" : "it is a Free Claim now"));
            }
        }
        for (ClaimView claim : listed()) {
            if (prices.find(claim.id()).isEmpty() || claim.free()) {
                claims.setForSale(claim.id(), false);
                changed(claim.id());
                changes.accept("Took " + claim.id() + " off sale: it has no price");
            }
        }
        for (ClaimListing listing : listings.all()) {
            Optional<ClaimView> claim = claims.find(listing.claimId());
            if (claim.isEmpty() || !isOurs(claim.get()) || !claim.get().isOwned()) {
                listings.remove(listing.claimId());
                changes.accept("Dropped the owner listing of " + listing.claimId() + ": "
                        + (claim.isPresent() && isOurs(claim.get()) ? "it has no owners left, so the server sells it" : "it is no longer on sale"));
            }
        }
    }

    /** @return whether SkysongEconomy put the Claim on sale */
    public boolean isOurs(ClaimView claim) {
        return claim.isForSaleBy(SELLER);
    }

    private void changed(String claimId) {
        listeners.forEach(listener -> listener.accept(claimId));
    }

    private ClaimView require(String claimId) {
        return claims.find(claimId).orElseThrow(() -> ClaimException.about("claims.unknown-claim", claimId));
    }

    private static void refuseNonOwner(ClaimView claim, CharacterRef character) {
        if (!claim.isOwnedBy(character)) {
            throw ClaimException.about("claims.sell.not-owner", claim.id());
        }
    }

    private static void refuseFree(ClaimView claim) {
        if (claim.free()) {
            throw ClaimException.about("claims.free", claim.id());
        }
    }
}
