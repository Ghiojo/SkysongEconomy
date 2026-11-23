package org.skysongdev.skysongEconomy.claims;

import java.util.List;
import java.util.UUID;

/** Where sellers' unpaid shares wait: the {@code claim_payouts} table. */
public interface ClaimPayoutRepository {
    /** @return every payout still owed */
    List<PendingPayout> loadAll();

    void save(PendingPayout payout);

    void delete(long saleId, UUID account);
}
