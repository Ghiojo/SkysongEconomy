package org.skysongdev.skysongEconomy.commands.ledger;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.data.database.StorageException;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** What the /se ledger commands share: the price database, item lookup and error handling. */
final class LedgerSupport {
    final PriceDatabase prices;
    final MaterialIdResolver materialIds;
    final Messenger messenger;
    private final PriceSettings settings;
    private final Logger logger;

    LedgerSupport(PriceDatabase prices, PriceSettings settings, MaterialIdResolver materialIds, Messenger messenger,
                  Logger logger) {
        this.prices = prices;
        this.settings = settings;
        this.materialIds = materialIds;
        this.messenger = messenger;
        this.logger = logger;
    }

    /** @return the id typed as the first argument, else the held item's; tells the sender when there is neither */
    Optional<String> materialId(CommandSender sender, String[] args, int argsAfterId) {
        Optional<String> id = materialIds.fromArgsOrMainHand(sender, args, argsAfterId);
        if (id.isEmpty()) {
            messenger.send(sender, "ledger.no-material");
        }
        return id;
    }

    /** @return a {@code <value>} placeholder: the value truncated to the configured decimals */
    TagResolver value(BigDecimal value) {
        return Placeholder.unparsed("value", settings.truncate(value).toPlainString());
    }

    /** Runs a change and reports refusals and database errors to the sender. */
    void run(CommandSender sender, String what, Runnable action) {
        try {
            action.run();
        } catch (PriceException refusal) {
            messenger.send(sender, refusal);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to " + what, e);
            messenger.send(sender, "ledger.storage-error");
        }
    }
}
