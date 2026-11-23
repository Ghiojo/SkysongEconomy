package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.claims.ClaimSaleRepository;

/** Records Claim sales on the background writer, so a purchase never waits for the database. */
public final class AsyncClaimSaleRepository implements ClaimSaleRepository {
    private final ClaimSaleRepository delegate;
    private final BackgroundWriter writer;

    public AsyncClaimSaleRepository(ClaimSaleRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public long lastId() {
        return delegate.lastId();
    }

    @Override
    public void save(ClaimSale sale) {
        writer.run("record the sale of the Claim " + sale.claimId(), () -> delegate.save(sale));
    }
}
