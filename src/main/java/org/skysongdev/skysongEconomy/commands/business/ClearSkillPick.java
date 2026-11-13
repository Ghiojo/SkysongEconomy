package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.SkillPick;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business clearpick &lt;id&gt; &lt;number&gt;: staff free one of a business's skill picks, so
 * the owner can choose again. Picks are numbered from 1, oldest first, as the picks menu shows.
 */
final class ClearSkillPick extends BusinessSubCommand {
    ClearSkillPick(BusinessCommandSupport support) {
        super(support, "clearpick", "<id> <number>", 2, BusinessCommandSupport.ADMIN_PERMISSION);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        SkillPick cleared = support.businesses.clearPick(business, parseNumber(args[1], business));
        support.messenger.send(sender, "business.picks.cleared", unparsed("number", args[1]),
                unparsed("skill", cleared.skillId()), unparsed("business", business.displayName()));
        support.reportChanges(sender, business);
    }

    private static int parseNumber(String text, Business business) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new BusinessException("business.picks.no-such-pick", Map.of(
                    "number", text, "count", String.valueOf(business.picks().size())));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> support.businesses.find(args[0])
                    .map(business -> IntStream.rangeClosed(1, business.picks().size()).mapToObj(String::valueOf).toList())
                    .orElse(List.of());
            default -> List.of();
        };
    }
}
