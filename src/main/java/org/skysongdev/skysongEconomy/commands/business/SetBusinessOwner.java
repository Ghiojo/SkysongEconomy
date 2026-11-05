package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business setowner &lt;id&gt; &lt;player&gt; [profile]: staff give a business to another character. */
final class SetBusinessOwner extends BusinessSubCommand {
    SetBusinessOwner(BusinessCommandSupport support) {
        super(support, "setowner", "<id> <player> [profile]", 2, BusinessCommandSupport.ADMIN_PERMISSION);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.character(sender, args[1], optional(args, 2)).ifPresent(owner -> {
            support.businesses.setOwner(business, owner);
            support.messenger.send(sender, "business.owner-set",
                    unparsed("business", business.displayName()), unparsed("owner", support.describe(owner)));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return completeCharacterArgs(args);
    }
}
