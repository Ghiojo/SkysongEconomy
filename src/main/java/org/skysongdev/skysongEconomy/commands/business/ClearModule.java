package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickGroup;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business clearmodule &lt;id&gt; &lt;skill&gt; &lt;number&gt;: staff free one of a business's
 * module picks, so the owner can choose again. Modules are numbered from 1 within their skill,
 * oldest first, as {@code /se business modules} shows.
 */
final class ClearModule extends BusinessSubCommand {
    private final ModuleCommandText text;

    ClearModule(BusinessCommandSupport support, ModuleCommandText text) {
        super(support, "clearmodule", "<id> <skill> <number>", 3, BusinessCommandSupport.ADMIN_PERMISSION);
        this.text = text;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        String skill = args[1].toLowerCase(Locale.ROOT);
        int number;
        try {
            number = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new BusinessException("business.modules.no-such-pick", Map.of("number", args[2], "skill", skill,
                    "count", String.valueOf(picked(business, skill))));
        }
        ModulePick cleared = text.modules().clear(business, skill, number);
        support.messenger.send(sender, "business.modules.cleared", component("module", text.name(cleared)),
                unparsed("skill", skill), unparsed("business", business.displayName()));
        // The module's own NPCs and extra workers go with it; say which.
        support.reportChanges(sender, business);
    }

    private int picked(Business business, String skill) {
        return text.modules().groups(business).stream().filter(group -> group.skill().equals(skill))
                .mapToInt(group -> group.picks().size()).findFirst().orElse(0);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> support.businessIds(args[0]);
            case 2 -> support.businesses.find(args[0])
                    .map(business -> text.modules().groups(business).stream().map(ModulePickGroup::skill)
                            .filter(skill -> skill.startsWith(args[1].toLowerCase(Locale.ROOT))).toList())
                    .orElse(List.of());
            case 3 -> support.businesses.find(args[0])
                    .map(business -> IntStream.rangeClosed(1, picked(business, args[1].toLowerCase(Locale.ROOT)))
                            .mapToObj(String::valueOf).toList())
                    .orElse(List.of());
            default -> List.of();
        };
    }
}
