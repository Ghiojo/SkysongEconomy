package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Optional;

/**
 * What the Claim rules need from SkysongClaims. On the server it is
 * {@code integration/skysongclaims/SkysongClaimsAdapter}; tests use a fake. Everything runs on the
 * main thread. Methods that take a Claim id throw {@link IllegalArgumentException} when no Claim
 * has it, so callers check with {@link #find} first.
 */
public interface ClaimGateway {
    /** @return the Claim with that id, ignoring case; empty when there is none */
    Optional<ClaimView> find(String claimId);

    /** @return every Claim */
    List<ClaimView> all();

    /** @return whether SkysongClaims has a Claim type with that id, as of its last reload */
    boolean typeExists(String typeId);

    /** @return whether the character may become an owner of the Claim now, and if not, why */
    OwnershipAnswer canOwn(CharacterRef character, String claimId);

    /**
     * Makes the character an owner, if {@link #canOwn} allows it.
     *
     * @return {@link OwnershipAnswer#OK} when it is now an owner, otherwise why it isn't
     */
    OwnershipAnswer addOwner(String claimId, CharacterRef character);

    /** Removes every owner. The Claim's build groups are cleared too. */
    void clearOwners(String claimId);

    /** Puts the Claim on sale in SkysongEconomy's name, or takes it off sale. */
    void setForSale(String claimId, boolean forSale);

    /**
     * Sets the fourth line of every sign of the Claim. SkysongClaims saves it, so it stays across
     * restarts; a sign linked later starts blank.
     *
     * @param miniMessage the line, in MiniMessage, with every placeholder already filled in
     */
    void setPriceLine(String claimId, String miniMessage);

    /** Blanks the fourth line of every sign of the Claim. */
    void clearPriceLine(String claimId);
}
