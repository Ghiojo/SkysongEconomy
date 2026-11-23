package org.skysongdev.skysongEconomy.claims;

import java.util.List;

/** Where owners' listings are kept: the {@code claim_listings} table. */
public interface ClaimListingRepository {
    /** @return every saved listing */
    List<ClaimListing> loadAll();

    /** Saves a listing, replacing the Claim's previous one. */
    void save(ClaimListing listing);

    void delete(String claimId);
}
