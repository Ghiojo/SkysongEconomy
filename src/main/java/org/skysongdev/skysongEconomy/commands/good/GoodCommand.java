package org.skysongdev.skysongEconomy.commands.good;

import org.skysongdev.skysongEconomy.commands.CommandGroup;
import org.skysongdev.skysongEconomy.data.database.CachedGoodTypes;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.logging.Logger;

/** /se good: tag Skysong items with good types. */
public final class GoodCommand extends CommandGroup {
    public GoodCommand(CachedGoodTypes goodTypes, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super("good", "skysongeconomy.good", messenger);
        register(new AddGoodAssociation(goodTypes, materialIds, messenger, logger));
        register(new RemoveGoodAssociation(goodTypes, materialIds, messenger, logger));
    }
}
