package org.skysongdev.skysongEconomy.commands.ledger;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.data.database.PriceLedgerRepository;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.logging.Logger;

/** /se ledger: view and edit the price of Skysong items. */
public final class LedgerCommand extends CommandGroup {
    public LedgerCommand(PriceLedgerRepository ledger, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super("ledger", "skysongeconomy.ledger", messenger);
        register(new ViewLedger(ledger, materialIds, messenger, logger));
        register(new AddLedgerEntry(ledger, materialIds, messenger, logger));
        register(new RemoveLedgerEntry(ledger, materialIds, messenger, logger));
    }
}
