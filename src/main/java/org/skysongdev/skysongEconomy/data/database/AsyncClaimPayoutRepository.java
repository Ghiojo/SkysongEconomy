package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.claims.ClaimPayoutRepository;
import org.skysongdev.skysongEconomy.claims.PendingPayout;

import java.util.List;
import java.util.UUID;

/** Saves sellers' unpaid shares on the background writer. */
public final class AsyncClaimPayoutRepository implements ClaimPayoutRepository {
    private final ClaimPayoutRepository delegate;
    private final BackgroundWriter writer;

    public AsyncClaimPayoutRepository(ClaimPayoutRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<PendingPayout> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(PendingPayout payout) {
        writer.run("save a payout for the Claim " + payout.claimId(), () -> delegate.save(payout));
    }

    @Override
    public void delete(long saleId, UUID account) {
        writer.run("delete the payout of sale " + saleId, () -> delegate.delete(saleId, account));
    }
}
