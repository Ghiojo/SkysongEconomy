package org.skysongdev.skysongEconomy.claims;

/**
 * Told when a seller's share of a Claim sale that waited for them is paid, so other plugins can
 * hear about it. Called on the main thread.
 */
@FunctionalInterface
public interface ClaimPayoutEvents {
    /** Tells nobody. */
    ClaimPayoutEvents NONE = payout -> {
    };

    /** @param payout the share now in the seller's balance */
    void paidOut(PendingPayout payout);
}
