package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business rename &lt;id&gt; &lt;name...&gt;: the owner or staff change the name players see. */
final class RenameBusiness extends BusinessSubCommand {
    RenameBusiness(BusinessCommandSupport support) {
        super(support, "rename", "<id> <name...>", 2, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        support.businesses.rename(business, joinFrom(args, 1));
        support.messenger.send(sender, "business.renamed", unparsed("business", business.displayName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
