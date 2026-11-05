package org.skysongdev.skysongEconomy.commands.ledger;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.data.database.PriceLedgerRepository;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** /se ledger add [materialId] &lt;value&gt;: sets the ledger price of an item. */
final class AddLedgerEntry implements SubCommand {
    private final PriceLedgerRepository ledger;
    private final MaterialIdResolver materialIds;
    private final Messenger messenger;
    private final Logger logger;

    AddLedgerEntry(PriceLedgerRepository ledger, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        this.ledger = ledger;
        this.materialIds = materialIds;
        this.messenger = messenger;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    public String permission() {
        return "skysongeconomy.ledger.add";
    }

    @Override
    public String usage() {
        return "[materialId] <value>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messenger.error(sender, "Usage: /se ledger add <usage> (hold the item to leave out the material ID)",
                    Placeholder.unparsed("usage", usage()));
            return;
        }
        Optional<String> materialId = materialIds.fromArgsOrMainHand(sender, args, 1);
        if (materialId.isEmpty()) {
            messenger.error(sender, "Specify a material ID or hold an item that has one.");
            return;
        }

        String valueArgument = args[args.length == 1 ? 0 : 1];
        float value;
        try {
            value = Float.parseFloat(valueArgument);
        } catch (NumberFormatException e) {
            messenger.error(sender, "<yellow><value></yellow> is not a number.", Placeholder.unparsed("value", valueArgument));
            return;
        }

        try {
            ledger.setCost(materialId.get(), value);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save the ledger price of " + materialId.get(), e);
            messenger.error(sender, "A database error occurred while saving the ledger entry.");
            return;
        }
        messenger.success(sender, "Ledger entry for <yellow><material></yellow> set to <gold><value></gold>.",
                Placeholder.unparsed("material", materialId.get()), Placeholder.unparsed("value", String.valueOf(value)));
    }
}
