package org.skysongdev.skysongEconomy.journal;

import java.util.Optional;

/**
 * The names of ledgers, their imports and exports, and piers, so records keep the names they had.
 * Every lookup is empty for something no longer loaded.
 */
public interface TradeNames {
    /** Knows no names: records keep the ids. */
    TradeNames NONE = new TradeNames() {
    };

    default Optional<String> ledger(String ledgerId) {
        return Optional.empty();
    }

    default Optional<String> importOffer(String ledgerId, String importId) {
        return Optional.empty();
    }

    default Optional<String> exportOffer(String ledgerId, String exportId) {
        return Optional.empty();
    }
}
