package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.api.claims.AppliedModifier;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.Wallet;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Buying a Claim SkysongEconomy has on sale. Main thread only.
 * <ol>
 *     <li>The Claim is quoted again; if what it costs changed since the buyer saw it, nothing happens.</li>
 *     <li>SkysongClaims is asked whether the character may own it (type limits and so on).</li>
 *     <li>The total is taken from the buyer.</li>
 *     <li>The character is made an owner. If SkysongClaims refuses now, the buyer gets the money back.</li>
 *     <li>The Claim comes off sale, the sale is recorded, and listeners are told (the plugin fires
 *         {@link org.skysongdev.skysongEconomy.api.claims.ClaimPurchaseEvent}).</li>
 * </ol>
 * In a server sale nobody is paid: the money leaves the economy, and a plugin that wants to keep
 * some of it (a treasury) does so from the event.
 */
public final class ClaimPurchases {
    private final ClaimGateway claims;
    private final ClaimSales sales;
    private final ClaimPricing pricing;
    private final Wallet wallet;
    private final ClaimSaleRepository repository;
    private final Consumer<ClaimSale> purchased;
    private final Consumer<String> warnings;
    private final Clock clock;
    private long lastId;

    /**
     * @param purchased told about every finished sale, after it is recorded
     * @param warnings  told when a refund fails, so staff can pay the buyer back by hand
     */
    public ClaimPurchases(ClaimGateway claims, ClaimSales sales, ClaimPricing pricing, Wallet wallet,
                          ClaimSaleRepository repository, Consumer<ClaimSale> purchased, Consumer<String> warnings,
                          Clock clock) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.sales = Objects.requireNonNull(sales, "sales");
        this.pricing = Objects.requireNonNull(pricing, "pricing");
        this.wallet = Objects.requireNonNull(wallet, "wallet");
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
     * @throws ClaimException when it isn't on sale by SkysongEconomy, or the character can't own it
     * @throws PriceException when its price can't be worked out
     */
    public ClaimQuote offer(CharacterRef buyer, String claimId) {
        ClaimView claim = claims.find(claimId).orElseThrow(() -> ClaimException.about("claims.unknown-claim", claimId));
        if (!sales.isOurs(claim)) {
            throw ClaimException.about("claims.buy.not-for-sale", claim.id());
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
        OwnershipAnswer added = claims.addOwner(quote.claimId(), buyer);
        if (added != OwnershipAnswer.OK) {
            refund(buyer, quote);
            refuseOwnership(added, quote.claimId());
            throw ClaimException.about("claims.buy.failed", quote.claimId());
        }
        sales.sold(quote.claimId());
        ClaimSale sale = new ClaimSale(++lastId, quote.claimId(), quote.claimTypeId(), buyer, quote.askingPrice(),
                quote.shares().stream().map(ClaimPurchases::applied).toList(), quote.modifierTotal(), quote.total(),
                clock.instant());
        repository.save(sale);
        purchased.accept(sale);
        return sale;
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
