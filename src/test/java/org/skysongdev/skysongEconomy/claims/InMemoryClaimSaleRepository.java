package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.api.claims.ClaimSale;

import java.util.ArrayList;
import java.util.List;

/** Keeps recorded sales in a list, starting after {@link #lastId}. */
final class InMemoryClaimSaleRepository implements ClaimSaleRepository {
    final List<ClaimSale> saved = new ArrayList<>();
    long lastId;

    @Override
    public long lastId() {
        return lastId;
    }

    @Override
    public void save(ClaimSale sale) {
        saved.add(sale);
    }
}
