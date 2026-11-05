package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.trade.LedgerSelectionRepository;
import org.skysongdev.skysongEconomy.trade.SelectedLedger;

import java.util.List;

/** Runs another repository's writes on a {@link BackgroundWriter}. Loading stays on the calling thread. */
public final class AsyncLedgerSelectionRepository implements LedgerSelectionRepository {
    private final LedgerSelectionRepository delegate;
    private final BackgroundWriter writer;

    public AsyncLedgerSelectionRepository(LedgerSelectionRepository delegate, BackgroundWriter writer) {
        this.delegate = delegate;
        this.writer = writer;
    }

    @Override
    public List<SelectedLedger> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void save(SelectedLedger selection) {
        writer.run("save ledger " + selection.ledgerId() + " of " + selection.businessId(), () -> delegate.save(selection));
    }

    @Override
    public void delete(String businessId, String ledgerId) {
        writer.run("remove ledger " + ledgerId + " of " + businessId, () -> delegate.delete(businessId, ledgerId));
    }
}
