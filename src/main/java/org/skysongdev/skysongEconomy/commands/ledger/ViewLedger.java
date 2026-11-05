package org.skysongdev.skysongEconomy.commands.ledger;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.data.database.Database;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** /se ledger view [materialId]: shows the ledger price of an item. */
final class ViewLedger implements SubCommand {
    private final Database database;
    private final MaterialIdResolver materialIds;
    private final Messenger messenger;
    private final Logger logger;

    ViewLedger(Database database, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        this.database = database;
        this.materialIds = materialIds;
        this.messenger = messenger;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "view";
    }

    @Override
    public String usage() {
        return "[materialId]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Optional<String> materialId = materialIds.fromArgsOrMainHand(sender, args, 0);
        if (materialId.isEmpty()) {
            messenger.error(sender, "Specify a material ID or hold an item that has one.");
            return;
        }

        float cost;
        try {
            cost = database.findItemCost(materialId.get());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to read the ledger price of " + materialId.get(), e);
            messenger.error(sender, "An error occurred while trying to retrieve the item's cost.");
            return;
        }

        if (cost == -1) {
            messenger.error(sender, "<yellow><material></yellow> does not have a cost set in the ledger.",
                    Placeholder.unparsed("material", materialId.get()));
            return;
        }
        messenger.success(sender, "The current cost of <yellow><material></yellow> is <gold><cost></gold>.",
                Placeholder.unparsed("material", materialId.get()), Placeholder.unparsed("cost", String.valueOf(cost)));
    }
}
