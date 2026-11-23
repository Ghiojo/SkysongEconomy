package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.api.claims.AppliedModifier;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.api.claims.Seller;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Buying a Claim SkysongEconomy has on sale. Main thread only.
 * <ol>
 *     <li>The Claim is quoted again; if what it costs changed since the buyer saw it, nothing happens.</li>
 *     <li>SkysongClaims is asked whether the character may own it (type limits and so on).</li>
 *     <li>The total is taken from the buyer.</li>
 *     <li>The character is made the owner: in a resale the old owners are removed first (which also
 *         clears the Claim's build groups). If SkysongClaims refuses now, the old owners are put back
 *         and the buyer gets the money back.</li>
 *     <li>In a resale (an owner listing), the asking price is split between the old owners
 *         ({@link SellerSplit}). A share the wallet can't pay now waits in {@link ClaimPayouts}.</li>
 *     <li>The Claim comes off sale, the sale is recorded, and listeners are told (the plugin fires
 *         {@link org.skysongdev.skysongEconomy.api.claims.ClaimPurchaseEvent}).</li>
 * </ol>
 * The modifiers' part never goes to the sellers. In a server sale nobody is paid: the money leaves
 * the economy, and a plugin that wants to keep some of it (a treasury) does so from the event.
 */
public final class ClaimPurchases {
    private final ClaimGateway claims;
    private final ClaimSales sales;
    private final ClaimPricing pricing;
    private final Wallet wallet;
    private final ClaimPayouts payouts;
    private final PriceSettings settings;
    private final ClaimSaleRepository repository;
    private final Consumer<ClaimSale> purchased;
    private final Consumer<String> warnings;
    private final Clock clock;
    private long lastId;

    /**
     * @param payouts   where sellers' shares wait when they can't be paid at once
     * @param settings  how sellers' shares are truncated
     * @param purchased told about every finished sale, after it is recorded
     * @param warnings  told when a refund or giving a Claim back fails, so staff can fix it by hand
     */
    public ClaimPurchases(ClaimGateway claims, ClaimSales sales, ClaimPricing pricing, Wallet wallet,
                          ClaimPayouts payouts, PriceSettings settings, ClaimSaleRepository repository,
                          Consumer<ClaimSale> purchased, Consumer<String> warnings, Clock clock) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.sales = Objects.requireNonNull(sales, "sales");
        this.pricing = Objects.requireNonNull(pricing, "pricing");
        this.wallet = Objects.requireNonNull(wallet, "wallet");
        this.payouts = Objects.requireNonNull(payouts, "payouts");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.purchased = Objects.requireNonNull(purchased, "purchased");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Reads the number of the last recorded sale, so new sales count on from it. Call once at start-up. */
    public void load() {
        lastId = repository.lastId();
    }

    /** @return whether SkysongEconomy is selling the Claim, so it can be bought here */
    public boolean isForSale(ClaimView claim) {
        return sales.isOurs(claim);
    }

    /** @return the owner listing the Claim is on sale under, empty for a server sale */
    public Optional<ClaimListing> listing(String claimId) {
        return sales.listings().find(claimId);
    }

    /**
     * @return what anyone buying the Claim pays now, modifiers included
     * @throws ClaimException or {@link PriceException} when it has no price or it can't be worked out
     */
    public BigDecimal price(String claimId) {
        return pricing.quote(claimId).total();
    }

    /**
     * What buying the Claim would cost the character now, checking first that they can.
     *
     * @throws ClaimException when it isn't on sale by SkysongEconomy, one of the player's characters
     *                        owns it, or the character can't own it
     * @throws PriceException when its price can't be worked out
     */
    public ClaimQuote offer(CharacterRef buyer, String claimId) {
        ClaimView claim = claims.find(claimId).orElseThrow(() -> ClaimException.about("claims.unknown-claim", claimId));
        if (!sales.isOurs(claim)) {
            throw ClaimException.about("claims.buy.not-for-sale", claim.id());
        }
        if (claim.owners().stream().anyMatch(owner -> owner.player().equals(buyer.player()))) {
            throw ClaimException.about("claims.buy.own-claim", claim.id());
        }
        refuseOwnership(claims.canOwn(buyer, claim.id()), claim.id());
        return pricing.quote(claim.id());
    }

    /**
     * Buys the Claim for the character, from their player's money.
     *
     * @param expectedTotal what the buyer was shown; the purchase is refused if the Claim costs anything else now
     * @return the finished sale
     * @throws ClaimException why it couldn't be bought; nothing changed then
     * @throws PriceException when its price can't be worked out
     */
    public ClaimSale buy(CharacterRef buyer, String claimId, BigDecimal expectedTotal) {
        ClaimQuote quote = offer(buyer, claimId);
        if (quote.total().compareTo(expectedTotal) != 0) {
            throw new ClaimException("claims.buy.price-changed", Map.of("claim", quote.claimId(),
                    "price", wallet.format(quote.total())));
        }
        if (!wallet.withdraw(buyer.player(), quote.total())) {
            throw new ClaimException("claims.buy.no-money", Map.of("claim", quote.claimId(),
                    "price", wallet.format(quote.total()), "balance", wallet.format(wallet.balance(buyer.player()))));
        }
        List<CharacterRef> oldOwners = claims.find(quote.claimId()).orElseThrow().owners();
        Optional<ClaimListing> listing = sales.listings().find(quote.claimId());
        OwnershipAnswer added = makeOwner(quote.claimId(), buyer, oldOwners);
        if (added != OwnershipAnswer.OK) {
            refund(buyer, quote);
            refuseOwnership(added, quote.claimId());
            throw ClaimException.about("claims.buy.failed", quote.claimId());
        }
        sales.sold(quote.claimId());
        long id = ++lastId;
        Instant now = clock.instant();
        Optional<Seller> seller = listing.filter(found -> !oldOwners.isEmpty()).map(found -> new Seller(found.listedBy(),
                oldOwners, SellerSplit.of(quote.askingPrice(), oldOwners, found.listedBy(), settings)));
        boolean paidLater = seller.map(found -> paySellers(id, quote.claimId(), found, now)).orElse(false);
        ClaimSale sale = new ClaimSale(id, quote.claimId(), quote.claimTypeId(), buyer, seller, quote.askingPrice(),
                quote.shares().stream().map(ClaimPurchases::applied).toList(), quote.modifierTotal(), quote.total(),
                paidLater, now);
        repository.save(sale);
        purchased.accept(sale);
        return sale;
    }

    /**
     * Makes the buyer the Claim's only owner. Old owners are removed first and put back if
     * SkysongClaims refuses the buyer, so a refused purchase changes nothing.
     */
    private OwnershipAnswer makeOwner(String claimId, CharacterRef buyer, List<CharacterRef> oldOwners) {
        if (oldOwners.isEmpty()) {
            return claims.addOwner(claimId, buyer);
        }
        return sales.transfer(claimId, () -> {
            claims.clearOwners(claimId);
            OwnershipAnswer added = claims.addOwner(claimId, buyer);
            if (added != OwnershipAnswer.OK) {
                oldOwners.stream().filter(owner -> claims.addOwner(claimId, owner) != OwnershipAnswer.OK)
                        .forEach(owner -> warnings.accept("The Claim " + claimId + " could not be given back to "
                                + owner.profile() + " (" + owner.player() + ") after a refused sale. Add them back by hand."));
            }
            return added;
        });
    }

    /** @return whether some share couldn't be paid now and waits until its seller joins */
    private boolean paySellers(long saleId, String claimId, Seller seller, Instant soldAt) {
        boolean later = false;
        for (Map.Entry<UUID, BigDecimal> payout : seller.payouts().entrySet()) {
            if (payout.getValue().signum() > 0 && !wallet.deposit(payout.getKey(), payout.getValue())) {
                payouts.owe(new PendingPayout(saleId, payout.getKey(), claimId, payout.getValue(), soldAt));
                later = true;
            }
        }
        return later;
    }

    private void refund(CharacterRef buyer, ClaimQuote quote) {
        if (!wallet.deposit(buyer.player(), quote.total())) {
            warnings.accept("Refund failed: " + buyer.player() + " (" + buyer.profile() + ") paid "
                    + wallet.format(quote.total()) + " for the Claim " + quote.claimId()
                    + " but SkysongClaims refused them as owner, and the money could not be given back. Pay it back by hand.");
        }
    }

    private static AppliedModifier applied(ClaimQuote.Share share) {
        PriceModifier modifier = share.modifier();
        return new AppliedModifier(modifier.id(), modifier.type(), modifier.target().key(), modifier.amount().kind(),
                modifier.amount().value(), share.amount(), modifier.displayName());
    }

    /** @throws ClaimException unless SkysongClaims said yes */
    private static void refuseOwnership(OwnershipAnswer answer, String claimId) {
        String key = switch (answer) {
            case OK -> null;
            case TYPE_LIMIT -> "claims.buy.type-limit";
            case FULL -> "claims.buy.full";
            case ALREADY_OWNER -> "claims.buy.already-owner";
            case UNKNOWN_CHARACTER -> "claims.buy.unknown-character";
        };
        if (key != null) {
            throw ClaimException.about(key, claimId);
        }
    }
}
