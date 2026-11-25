package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.api.trade.LedgerSelectionChange;
import org.skysongdev.skysongEconomy.business.Business;

/**
 * Told when a ledger selection changes, after it is saved, so other plugins can hear about it.
 * Called on the main thread. Selections dropped because their business was deleted are not
 * reported: the business's deletion is.
 */
public interface LedgerSelectionEvents {
    /** Tells nobody. */
    LedgerSelectionEvents NONE = new LedgerSelectionEvents() {
    };

    /** @param change what changed on that business */
    default void changed(Business business, LedgerSelectionChange change) {
    }
}
