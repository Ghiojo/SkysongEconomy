package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business hire &lt;id&gt; &lt;player&gt; [profile]: the owner or staff hire a character. Without
 * a profile, the character the player is playing now is hired.
 */
final class HireWorker extends BusinessSubCommand {
    HireWorker(BusinessCommandSupport support) {
        super(support, "hire", "<id> <player> [profile]", 2, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        support.character(sender, args[1], optional(args, 2)).ifPresent(character -> {
            support.businesses.hire(business, character);
            support.messenger.send(sender, "business.hired",
                    unparsed("character", support.describe(character)), unparsed("business", business.displayName()));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return completeCharacterArgs(args);
    }
}
