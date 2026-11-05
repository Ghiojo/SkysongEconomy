package org.skysongdev.skysongEconomy.commands.good;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;
import org.skysongdev.skysongEconomy.data.database.CachedGoodTypes;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared argument handling for {@code /se good add|remove [materialId] <goodType>}.
 * Subclasses only say what to do with the resolved material ID and good type.
 */
abstract class GoodAssociationCommand implements SubCommand {
    protected final CachedGoodTypes goodTypes;
    private final MaterialIdResolver materialIds;
    private final Messenger messenger;
    private final Logger logger;

    GoodAssociationCommand(CachedGoodTypes goodTypes, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        this.goodTypes = goodTypes;
        this.materialIds = materialIds;
        this.messenger = messenger;
        this.logger = logger;
    }

    /** Applies the change; returns false when there was nothing to change. */
    protected abstract boolean apply(String materialId, String goodType) throws SQLException;

    /** MiniMessage shown when the change was made; may use {@code <material>} and {@code <good_type>}. */
    protected abstract String successMessage();

    /** MiniMessage shown when there was nothing to change; may use the same placeholders. */
    protected abstract String unchangedMessage();

    @Override
    public final String permission() {
        return "skysongeconomy.good.manage";
    }

    @Override
    public final String usage() {
        return "[materialId] <goodType>";
    }

    @Override
    public final void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messenger.error(sender, "Usage: /se good <name> <usage> (hold the item to leave out the material ID)",
                    Placeholder.unparsed("name", name()), Placeholder.unparsed("usage", usage()));
            return;
        }
        Optional<String> materialId = materialIds.fromArgsOrMainHand(sender, args, 1);
        if (materialId.isEmpty()) {
            messenger.error(sender, "Specify a material ID or hold an item that has one.");
            return;
        }
        String goodType = args[args.length == 1 ? 0 : 1];

        boolean changed;
        try {
            changed = apply(materialId.get(), goodType);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to " + name() + " good type " + goodType + " on " + materialId.get(), e);
            messenger.error(sender, "A database error occurred while updating the good type.");
            return;
        }
        var material = Placeholder.unparsed("material", materialId.get());
        var goodTypePlaceholder = Placeholder.unparsed("good_type", goodType);
        if (changed) {
            messenger.success(sender, successMessage(), material, goodTypePlaceholder);
        } else {
            messenger.error(sender, unchangedMessage(), material, goodTypePlaceholder);
        }
    }
}
