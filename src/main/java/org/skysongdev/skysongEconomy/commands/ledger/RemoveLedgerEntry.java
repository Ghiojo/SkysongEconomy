package org.skysongdev.skysongEconomy.commands.ledger;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.commands.SubCommand;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se ledger remove [materialId]: removes an item's base value, unless another item's uses it. */
final class RemoveLedgerEntry implements SubCommand {
    private final LedgerSupport support;

    RemoveLedgerEntry(LedgerSupport support) {
        this.support = support;
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
        support.materialId(sender, args, 0).ifPresent(id -> support.run(sender, "remove the base value of " + id, () -> {
            String key = support.prices.remove(id) ? "ledger.removed" : "ledger.not-found";
            support.messenger.send(sender, key, unparsed("material", id));
        }));
    }
}
