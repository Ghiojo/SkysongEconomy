package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business delete &lt;id&gt; [confirm]: staff delete a business. Without {@code confirm} it
 * only explains what will happen, so a typo can't delete one.
 */
final class DeleteBusiness extends BusinessSubCommand {
    private static final String CONFIRM = "confirm";

    DeleteBusiness(BusinessCommandSupport support) {
        super(support, "delete", "<id> [confirm]", 1, BusinessCommandSupport.ADMIN_PERMISSION);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        if (args.length < 2 || !args[1].equalsIgnoreCase(CONFIRM)) {
            support.messenger.send(sender, "business.delete-confirm",
                    unparsed("business", business.displayName()), unparsed("id", business.id()));
            return;
        }
        support.businesses.delete(business);
        support.messenger.send(sender, "business.deleted", unparsed("business", business.displayName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> List.of(CONFIRM);
            default -> List.of();
        };
    }
}
