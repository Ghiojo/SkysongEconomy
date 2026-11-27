package org.skysongdev.skysongEconomy.api.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A finished Claim purchase, as it was saved in the {@code claim_sales} table.
 * <p>
 * {@code paid = askingPrice + modifierTotal}. The asking price is what the seller asked; the
 * modifier part never goes to the seller, so a plugin that keeps that money (a treasury) takes it
 * from {@link #modifiers()}. In a server sale ({@link #seller()} empty) nobody is paid, and the
 * whole amount left the economy; in a resale the sellers received the asking price.
 *
 * @param id            the sale's number, counting up from 1
 * @param claimId       the Claim bought
 * @param claimTypeId   its type when it was bought
 * @param buyer         the character that now owns it; {@code buyer.player()} is the account that paid
 * @param seller        who sold it and what each received; empty when the server sold it
 * @param askingPrice   the Claim's own price
 * @param modifiers     every modifier that raised or lowered it, with its share, in the order they were set
 * @param modifierTotal the sum of the shares, negative when discounts outweigh the rest, never below
 *                      {@code -askingPrice}
 * @param paid          what the buyer paid
 * @param sellerPaidLater whether some seller's share couldn't be paid at once and waits until they join
 * @param at            when it was bought
 */
public record ClaimSale(long id, String claimId, String claimTypeId, CharacterRef buyer, Optional<Seller> seller,
                        BigDecimal askingPrice, List<AppliedModifier> modifiers, BigDecimal modifierTotal,
                        BigDecimal paid, boolean sellerPaidLater, Instant at) {
    public ClaimSale {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(claimTypeId, "claimTypeId");
        Objects.requireNonNull(buyer, "buyer");
        Objects.requireNonNull(seller, "seller");
        Objects.requireNonNull(askingPrice, "askingPrice");
        modifiers = List.copyOf(modifiers);
        Objects.requireNonNull(modifierTotal, "modifierTotal");
        Objects.requireNonNull(paid, "paid");
        Objects.requireNonNull(at, "at");
    }
}
