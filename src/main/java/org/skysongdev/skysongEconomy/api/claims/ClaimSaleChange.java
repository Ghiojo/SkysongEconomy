package org.skysongdev.skysongEconomy.api.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A change to a Claim's asking price, or to whether SkysongEconomy has it on sale.
 *
 * @param kind     what happened
 * @param claimId  the Claim
 * @param price    its asking price after the change, before modifiers; empty when it has none
 * @param listedBy the owner listing involved: the owner who put it on sale for {@link Kind#OWNER_LISTED},
 *                 or the listing that ended for {@link Kind#UNLISTED}, {@link Kind#SOLD} and
 *                 {@link Kind#OWNER_LISTING_ENDED}; empty for a server sale
 * @param at       when it happened
 */
public record ClaimSaleChange(Kind kind, String claimId, Optional<BigDecimal> price, Optional<CharacterRef> listedBy,
                              Instant at) {
    public ClaimSaleChange {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(listedBy, "listedBy");
        Objects.requireNonNull(at, "at");
    }

    /** What happened. New kinds may be added. */
    public enum Kind {
        /** Staff set or changed its asking price, without changing whether it is on sale. */
        PRICE_SET,
        /** Its asking price was removed. A Claim on sale first fires {@link #UNLISTED}. */
        PRICE_CLEARED,
        /** Staff put an unowned Claim on sale for the server. */
        SERVER_LISTED,
        /** An owner put their Claim on sale (or listed it again at a new price). */
        OWNER_LISTED,
        /** It was taken off sale, by an owner or staff, or because it had no price on start-up. */
        UNLISTED,
        /** It was bought and came off sale; {@code ClaimPurchaseEvent} has the sale itself. */
        SOLD,
        /** Every owner of an owner-listed Claim left, so it stays on sale for the server. */
        OWNER_LISTING_ENDED,
        /** A priced Claim lost its last owner and went back on sale for the server. */
        RELISTED,
        /** It was deleted in SkysongClaims, and its price and listing went with it. */
        DELETED
    }
}
