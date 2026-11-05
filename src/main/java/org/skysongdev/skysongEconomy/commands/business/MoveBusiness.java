package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business move &lt;id&gt;: the owner or staff move a business to the block they look at.
 * This also places a business whose block staff broke.
 */
final class MoveBusiness extends BusinessSubCommand {
    MoveBusiness(BusinessCommandSupport support) {
        super(support, "move", "<id>", 1, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        support.requirePlayer(sender).flatMap(support::targetBlock).ifPresent(block -> {
            support.businesses.move(business, block);
            support.messenger.send(sender, "business.moved", unparsed("business", business.displayName()));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
