package org.skysongdev.skysongEconomy.claims;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Claim prices kept in a map, as the table would keep them. */
final class InMemoryClaimPriceRepository implements ClaimPriceRepository {
    final Map<String, Row> rows = new LinkedHashMap<>();

    @Override
    public List<Row> loadAll() {
        return new ArrayList<>(rows.values());
    }

    @Override
    public void save(ClaimPrice price) {
        rows.put(price.claimId(), new Row(price.claimId(), price.expression().text(), price.setBy(),
                price.setAt().toEpochMilli()));
    }

    @Override
    public void delete(String claimId) {
        rows.remove(claimId);
    }
}
