package org.skysongdev.skysongEconomy.claims;

/** Whether a character may become an owner of a Claim, and if not, why; as SkysongClaims answers it. */
public enum OwnershipAnswer {
    /** It may, or it just did. */
    OK,
    /** It already owns as many Claims of the type as its limit allows. */
    TYPE_LIMIT,
    /** Every slot of the Free Claim is taken. Claims that are not free are never full. */
    FULL,
    /** It already owns the Claim. */
    ALREADY_OWNER,
    /** SkysongStats has no such character. */
    UNKNOWN_CHARACTER
}
