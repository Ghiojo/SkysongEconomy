package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business setowner &lt;id&gt; &lt;player|#server&gt; [profile]: staff give a business to another
 * character, or to the server with {@code #server}.
 */
final class SetBusinessOwner extends BusinessSubCommand {
    SetBusinessOwner(BusinessCommandSupport support) {
        super(support, "setowner", "<id> <player|#server> [profile]", 2, BusinessCommandSupport.ADMIN_PERMISSION);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        if (BusinessCommandSupport.isServerOwner(args[1])) {
            support.businesses.giveToServer(business);
            ownerSet(sender, business);
            return;
        }
        support.character(sender, args[1], optional(args, 2)).ifPresent(owner -> {
            support.businesses.setOwner(business, owner);
            ownerSet(sender, business);
        });
    }

    private void ownerSet(CommandSender sender, Business business) {
        support.messenger.send(sender, "business.owner-set",
                unparsed("business", business.displayName()), unparsed("owner", support.describeOwner(business)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> support.owners(args[1]);
            case 3 -> BusinessCommandSupport.isServerOwner(args[1]) ? List.of() : completeCharacterArgs(args);
            default -> completeCharacterArgs(args);
        };
    }
}
