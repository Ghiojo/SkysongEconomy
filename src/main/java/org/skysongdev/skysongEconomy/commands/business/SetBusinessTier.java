package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.business.TierChange;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business settier &lt;id&gt; &lt;tier&gt;: staff upgrade or downgrade a business. */
final class SetBusinessTier extends BusinessSubCommand {
    SetBusinessTier(BusinessCommandSupport support) {
        super(support, "settier", "<id> <tier>", 2, BusinessCommandSupport.ADMIN_PERMISSION);
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        TierChange change = support.businesses.setTier(business, parseTier(args[1]));
        support.messenger.send(sender, "business.tier-set",
                unparsed("business", business.displayName()), unparsed("tier", String.valueOf(business.tier())));
        if (!change.removedWorkers().isEmpty()) {
            String names = change.removedWorkers().stream().map(hire -> support.describe(hire.character()))
                    .collect(Collectors.joining(", "));
            support.messenger.send(sender, "business.tier-removed-workers", unparsed("workers", names));
        }
        if (!change.removedPicks().isEmpty()) {
            String skills = change.removedPicks().stream().map(SkillPick::skillId).collect(Collectors.joining(", "));
            support.messenger.send(sender, "business.tier-removed-picks", unparsed("skills", skills));
        }
    }

    private static int parseTier(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new BusinessException("business.invalid-tier", Map.of(
                    "min", String.valueOf(Business.MIN_TIER), "max", String.valueOf(Business.MAX_TIER)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> IntStream.rangeClosed(Business.MIN_TIER, Business.MAX_TIER).mapToObj(String::valueOf).toList();
            default -> List.of();
        };
    }
}
