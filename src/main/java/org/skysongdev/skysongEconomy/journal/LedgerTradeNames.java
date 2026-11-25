package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.trade.ExportOffer;
import org.skysongdev.skysongEconomy.trade.ImportOffer;
import org.skysongdev.skysongEconomy.trade.Ledger;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;

import java.util.Optional;

/** Names from the loaded ledger files. */
public final class LedgerTradeNames implements TradeNames {
    private final LedgerRegistry ledgers;

    public LedgerTradeNames(LedgerRegistry ledgers) {
        this.ledgers = ledgers;
    }

    @Override
    public Optional<String> ledger(String ledgerId) {
        return ledgers.find(ledgerId).map(Ledger::displayName);
    }

    @Override
    public Optional<String> importOffer(String ledgerId, String importId) {
        return ledgers.find(ledgerId).flatMap(ledger -> Optional.ofNullable(ledger.imports().get(importId)))
                .map(ImportOffer::displayName);
    }

    @Override
    public Optional<String> exportOffer(String ledgerId, String exportId) {
        return ledgers.find(ledgerId).flatMap(ledger -> Optional.ofNullable(ledger.exports().get(exportId)))
                .map(ExportOffer::displayName);
    }
}
