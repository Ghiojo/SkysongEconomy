package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.Collection;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business list: staff see every business. */
final class ListBusinesses extends BusinessSubCommand {
    ListBusinesses(BusinessCommandSupport support) {
        super(support, "list", "", 0, BusinessCommandSupport.ADMIN_PERMISSION);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Collection<Business> all = support.businesses.all();
        support.messenger.send(sender, "business.list-header", unparsed("count", String.valueOf(all.size())));
        for (Business business : all) {
            support.messenger.send(sender, "business.list-entry",
                    unparsed("business", business.displayName()), unparsed("id", business.id()),
                    unparsed("type", business.typeId()), unparsed("tier", String.valueOf(business.tier())));
        }
    }
}
