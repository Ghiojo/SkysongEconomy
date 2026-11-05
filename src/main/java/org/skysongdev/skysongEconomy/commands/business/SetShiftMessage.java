package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business shiftmessage &lt;id&gt; &lt;message...|reset&gt;: the owner or staff set the text
 * announced when someone starts a shift. It may use {character}, {player} and {business}.
 */
final class SetShiftMessage extends BusinessSubCommand {
    private static final String RESET = "reset";

    SetShiftMessage(BusinessCommandSupport support) {
        super(support, "shiftmessage", "<id> <message...|reset>", 2, null);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        if (args.length == 2 && args[1].equalsIgnoreCase(RESET)) {
            support.businesses.setShiftMessage(business, null);
            support.messenger.send(sender, "business.shift-message-reset");
            return;
        }
        support.businesses.setShiftMessage(business, joinFrom(args, 1));
        support.messenger.send(sender, "business.shift-message-set",
                unparsed("message", business.shiftMessage().orElse("")));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> List.of(RESET, "{character}", "{player}", "{business}");
            default -> List.of();
        };
    }
}
