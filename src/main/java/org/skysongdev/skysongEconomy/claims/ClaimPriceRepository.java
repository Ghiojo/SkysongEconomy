package org.skysongdev.skysongEconomy.claims;

import java.util.List;

/** Where Claims' asking prices are saved. */
public interface ClaimPriceRepository {
    /**
     * A saved row, read as written. Its expression is only parsed when it is loaded, so one broken
     * row doesn't stop the others.
     */
    record Row(String claimId, String expression, String setBy, long setAtMillis) {
    }

    /** @return every saved price */
    List<Row> loadAll();

    /** Saves a new price or replaces the Claim's old one. */
    void save(ClaimPrice price);

    void delete(String claimId);
}
