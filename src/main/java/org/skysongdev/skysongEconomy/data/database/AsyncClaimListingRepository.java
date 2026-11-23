package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.claims.ClaimListing;
import org.skysongdev.skysongEconomy.claims.ClaimListingRepository;

import java.util.List;

/** Saves owners' Claim listings on the background writer. */
public final class AsyncClaimListingRepository implements ClaimListingRepository {
    private final ClaimListingRepository delegate;
    private final BackgroundWriter writer;

    public AsyncClaimListingRepository(ClaimListingRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<ClaimListing> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(ClaimListing listing) {
        writer.run("save the listing of the Claim " + listing.claimId(), () -> delegate.save(listing));
    }

    @Override
    public void delete(String claimId) {
        writer.run("delete the listing of the Claim " + claimId, () -> delegate.delete(claimId));
    }
}
