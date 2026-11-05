package org.skysongdev.skysongEconomy.commands.business;

import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;

import java.util.List;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** /se business info &lt;id&gt;: shows a business's type, tier, owner, workers and block. */
final class BusinessInfo extends BusinessSubCommand {
    private final BusinessTypeRegistry types;

    BusinessInfo(BusinessCommandSupport support, BusinessTypeRegistry types) {
        super(support, "info", "<id>", 1, null);
        this.types = types;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Business business = support.businesses.require(args[0]);
        String workers = business.workers().isEmpty()
                ? null
                : business.workers().stream().map(hire -> support.describe(hire.character())).collect(Collectors.joining(", "));
        support.messenger.send(sender, "business.info",
                unparsed("business", business.displayName()),
                unparsed("id", business.id()),
                parsed("type", types.find(business.typeId()).map(BusinessType::displayName).orElse(business.typeId())),
                unparsed("tier", String.valueOf(business.tier())),
                unparsed("owner", support.describeOwner(business)),
                unparsed("count", String.valueOf(business.workers().size())),
                unparsed("max", String.valueOf(support.businesses.maxWorkers(business))),
                workers == null ? component("workers", support.messenger.render("business.info-no-workers")) : unparsed("workers", workers),
                business.picks().isEmpty()
                        ? component("picks", support.messenger.render("business.info-no-workers"))
                        : unparsed("picks", business.picks().stream().map(SkillPick::skillId).collect(Collectors.joining(", "))),
                unparsed("free_picks", String.valueOf(business.freePicks())),
                business.position().isPresent()
                        ? unparsed("position", business.position().get().toString())
                        : component("position", support.messenger.render("business.info-no-block")));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? support.businessIds(args[0]) : List.of();
    }
}
