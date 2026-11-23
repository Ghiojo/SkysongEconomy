package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Instant;
import java.util.Objects;

/**
 * An owner put their Claim on sale. While it lasts, the Claim's owners are its sellers: they share
 * the asking price when it is bought.
 *
 * @param claimId  the Claim
 * @param listedBy the owner who put it on sale; they get any remainder when the price is split
 * @param listedAt when
 */
public record ClaimListing(String claimId, CharacterRef listedBy, Instant listedAt) {
    public ClaimListing {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(listedBy, "listedBy");
        Objects.requireNonNull(listedAt, "listedAt");
    }
}
