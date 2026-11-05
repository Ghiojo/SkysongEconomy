package org.skysongdev.skysongEconomy.commands.ledger;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.data.database.Database;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.logging.Logger;

/** /se ledger: view and edit the price of Skysong items. */
public final class LedgerCommand extends CommandGroup {
    public LedgerCommand(Database database, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super("ledger", "skysongeconomy.ledger", messenger);
        register(new ViewLedger(database, materialIds, messenger, logger));
        register(new AddLedgerEntry(database, materialIds, messenger, logger));
        register(new RemoveLedgerEntry(database, materialIds, messenger, logger));
    }
}
