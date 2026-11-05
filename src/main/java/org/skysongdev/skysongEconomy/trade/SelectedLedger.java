package org.skysongdev.skysongEconomy.trade;

import java.time.Instant;
import java.util.Objects;

/**
 * A ledger a business has in one of its ledger slots.
 *
 * @param selectedAt when the owner selected it; the newest go first when slots shrink
 */
public record SelectedLedger(String businessId, String ledgerId, Instant selectedAt) {
    public SelectedLedger {
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(ledgerId, "ledgerId");
        Objects.requireNonNull(selectedAt, "selectedAt");
    }
}
