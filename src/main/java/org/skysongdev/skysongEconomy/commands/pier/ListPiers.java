package org.skysongdev.skysongEconomy.commands.pier;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.api.piers.Pier;

import java.util.List;

/** /se pier list: every pier, open or closed, with its loss chance for honest cargo and where it comes from. */
final class ListPiers extends PierSubCommand {
    ListPiers(PierCommandSupport support) {
        super(support, "pier", "list", "", 0);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        List<Pier> piers = support.piers.all();
        if (piers.isEmpty()) {
            support.messenger.send(sender, "piers.list-empty");
            return;
        }
        support.messenger.send(sender, "piers.list-header", Placeholder.unparsed("count", Integer.toString(piers.size())));
        for (Pier pier : piers) {
            support.messenger.send(sender, pier.open() ? "piers.list-entry" : "piers.list-entry-closed",
                    PierCommandSupport.pier(pier), PierCommandSupport.chance(pier),
                    Placeholder.unparsed("source", pier.source()));
        }
    }
}
