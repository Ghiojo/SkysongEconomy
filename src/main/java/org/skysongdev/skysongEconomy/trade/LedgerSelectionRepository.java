package org.skysongdev.skysongEconomy.trade;

import java.util.List;

/** Where each business's selected ledgers are saved. */
public interface LedgerSelectionRepository {
    /** @return every selection, of every business */
    List<SelectedLedger> loadAll();

    void save(SelectedLedger selection);

    void delete(String businessId, String ledgerId);
}
