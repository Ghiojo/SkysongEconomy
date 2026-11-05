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

/** /se ledger remove [materialId]: removes an item from the ledger. */
final class RemoveLedgerEntry implements SubCommand {
    private final Database database;
    private final MaterialIdResolver materialIds;
    private final Messenger messenger;
    private final Logger logger;

    RemoveLedgerEntry(Database database, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        this.database = database;
        this.materialIds = materialIds;
        this.messenger = messenger;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    public String permission() {
        return "skysongeconomy.ledger.remove";
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

        try {
            database.deleteLedgerData(materialId.get());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to remove the ledger entry of " + materialId.get(), e);
            messenger.error(sender, "Failed to remove the ledger entry due to a database error.");
            return;
        }
        messenger.success(sender, "Removed the ledger entry for <yellow><material></yellow>.",
                Placeholder.unparsed("material", materialId.get()));
    }
}
