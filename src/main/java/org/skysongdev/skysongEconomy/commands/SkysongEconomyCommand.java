package org.skysongdev.skysongEconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.commands.good.GoodCommand;
import org.skysongdev.skysongEconomy.commands.ledger.LedgerCommand;
import org.skysongdev.skysongEconomy.data.database.GoodTypeRepository;
import org.skysongdev.skysongEconomy.data.database.PriceLedgerRepository;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;
import java.util.logging.Logger;

/** The /se command. Bukkit entry point that hands every call to the command tree. */
public final class SkysongEconomyCommand implements TabExecutor {
    private final CommandGroup root;

    public SkysongEconomyCommand(PriceLedgerRepository ledger, GoodTypeRepository goodTypes, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        this.root = new CommandGroup("se", null, messenger)
                .register(new LedgerCommand(ledger, materialIds, messenger, logger))
                .register(new GoodCommand(goodTypes, materialIds, messenger, logger));
    }

    /** Adds a feature's command, such as {@code /se business}, under {@code /se}. */
    public void register(SubCommand feature) {
        root.register(feature);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        root.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return root.tabComplete(sender, args);
    }
}
