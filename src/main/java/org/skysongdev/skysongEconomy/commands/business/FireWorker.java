package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business fire &lt;id&gt; &lt;player&gt; [profile]: the owner or staff remove a worker. */
final class FireWorker extends BusinessSubCommand {
    FireWorker(BusinessCommandSupport support) {
        super(support, "fire", "<id> <player> [profile]", 2, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        support.character(sender, args[1], optional(args, 2)).ifPresent(character -> {
            support.businesses.fire(business, character);
            support.messenger.send(sender, "business.fired",
                    unparsed("character", support.describe(character)), unparsed("business", business.displayName()));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return completeCharacterArgs(args);
    }
}
