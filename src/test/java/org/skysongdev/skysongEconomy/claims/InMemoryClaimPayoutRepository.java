package org.skysongdev.skysongEconomy.claims;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Keeps waiting payouts in a list, as the table would. */
final class InMemoryClaimPayoutRepository implements ClaimPayoutRepository {
    final List<PendingPayout> saved = new ArrayList<>();

    @Override
    public List<PendingPayout> loadAll() {
        return new ArrayList<>(saved);
    }

    @Override
    public void save(PendingPayout payout) {
        saved.add(payout);
    }

    @Override
    public void delete(long saleId, UUID account) {
        saved.removeIf(payout -> payout.saleId() == saleId && payout.account().equals(account));
    }
}
