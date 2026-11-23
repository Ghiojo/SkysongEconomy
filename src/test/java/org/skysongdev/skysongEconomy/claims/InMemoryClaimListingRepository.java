package org.skysongdev.skysongEconomy.claims;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps owner listings in a map, as the table would. */
final class InMemoryClaimListingRepository implements ClaimListingRepository {
    final Map<String, ClaimListing> saved = new LinkedHashMap<>();

    @Override
    public List<ClaimListing> loadAll() {
        return new ArrayList<>(saved.values());
    }

    @Override
    public void save(ClaimListing listing) {
        saved.put(listing.claimId(), listing);
    }

    @Override
    public void delete(String claimId) {
        saved.remove(claimId);
    }
}
