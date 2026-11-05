package org.skysongdev.skysongEconomy.commands.ledger;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.util.logging.Logger;

/** /se ledger: view and edit the base value of Skysong items. */
public final class LedgerCommand extends CommandGroup {
    public LedgerCommand(PriceDatabase prices, PriceSettings settings, MaterialIdResolver materialIds,
                         Messenger messenger, Logger logger) {
        super("ledger", "skysongeconomy.ledger", messenger);
        LedgerSupport support = new LedgerSupport(prices, settings, materialIds, messenger, logger);
        register(new ViewLedger(support));
        register(new ListLedger(support));
        register(new AddLedgerEntry(support));
        register(new RemoveLedgerEntry(support));
    }
}
