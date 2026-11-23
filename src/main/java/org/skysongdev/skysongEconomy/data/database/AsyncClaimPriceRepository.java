package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.claims.ClaimPrice;
import org.skysongdev.skysongEconomy.claims.ClaimPriceRepository;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncClaimPriceRepository implements ClaimPriceRepository {
    private final ClaimPriceRepository delegate;
    private final BackgroundWriter writer;

    public AsyncClaimPriceRepository(ClaimPriceRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<Row> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(ClaimPrice price) {
        writer.run("save the price of the Claim " + price.claimId(), () -> delegate.save(price));
    }

    @Override
    public void delete(String claimId) {
        writer.run("delete the price of the Claim " + claimId, () -> delegate.delete(claimId));
    }
}
