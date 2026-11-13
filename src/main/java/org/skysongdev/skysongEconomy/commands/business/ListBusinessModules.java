package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * /se business modules &lt;id&gt;: the owner or staff list a business's module picks, skill by
 * skill. Modules are numbered from 1 within their skill, oldest first; {@code clearmodule} takes
 * that number.
 */
final class ListBusinessModules extends BusinessSubCommand {
    private final ModuleCommandText text;

    ListBusinessModules(BusinessCommandSupport support, ModuleCommandText text) {
        super(support, "modules", "<id>", 1, null);
        this.text = text;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        support.requireManager(sender, business);
        if (!text.modules().hasPicks(business)) {
            support.messenger.send(sender, "business.modules.list-none", unparsed("business", business.displayName()));
            return;
        }
        support.messenger.send(sender, "business.modules.list-header", unparsed("business", business.displayName()));
        text.list(sender, business);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
