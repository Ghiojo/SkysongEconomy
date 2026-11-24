package org.skysongdev.skysongEconomy.api.trade;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A ledger put in, or taken out of, an Economy and Logistics business's slot.
 *
 * @param kind       what happened
 * @param businessId the business
 * @param ledgerId   the ledger, such as {@code smugglers}
 * @param fine       the removal fine paid; 0 when selecting, when dropped, or on a server-owned business
 * @param paidBy     the owner who paid the fine; empty when no fine was paid
 * @param at         when it happened
 */
public record LedgerSelectionChange(Kind kind, String businessId, String ledgerId, BigDecimal fine,
                                    Optional<CharacterRef> paidBy, Instant at) {
    public LedgerSelectionChange {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(ledgerId, "ledgerId");
        Objects.requireNonNull(fine, "fine");
        Objects.requireNonNull(paidBy, "paidBy");
        Objects.requireNonNull(at, "at");
    }

    /** What happened to the selection. New kinds may be added. */
    public enum Kind {
        /** The ledger was put in a free slot. Free. */
        SELECTED,
        /** The owner (or staff, for a server-owned business) took it out and paid the removal fine. */
        REMOVED,
        /** The business lost the slot (a lower tier, or a cleared pick), so the newest ledger went, without a fine. */
        DROPPED
    }
}
