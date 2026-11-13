package org.skysongdev.skysongEconomy.commands.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.command.CommandSender;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickGroup;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/**
 * What the module commands share: the module service and how modules are written in chat.
 *
 * @param modules the modules each business picked
 * @param skills  skill names
 */
record ModuleCommandText(BusinessModules modules, SkillCatalog skills, Messenger messenger) implements ChangeReport {
    /** @return the module's name with its formatting, or its id when its file is gone */
    Component name(ModulePick pick) {
        return modules.module(pick.moduleId()).map(Module::name).map(messenger::renderText)
                .orElseGet(() -> Component.text(pick.moduleId()));
    }

    /** Lists the business's modules skill by skill, numbered within each skill; silent when it has no module picks. */
    void list(CommandSender sender, Business business) {
        for (ModulePickGroup group : modules.groups(business)) {
            messenger.send(sender, "business.modules.list-group", parsed("skill", skills.displayName(group.skill())),
                    unparsed("id", group.skill()), unparsed("picked", String.valueOf(group.picks().size())),
                    unparsed("max", String.valueOf(Math.max(group.total(), group.picks().size()))));
            for (int i = 0; i < group.picks().size(); i++) {
                messenger.send(sender, "business.modules.list-module", unparsed("number", String.valueOf(i + 1)),
                        component("module", name(group.picks().get(i))), unparsed("id", group.picks().get(i).moduleId()));
            }
        }
    }

    /** Tells the sender which modules the business just lost with their picks, if any. */
    @Override
    public void report(CommandSender sender, Business business) {
        List<ModulePick> lost = modules.takeLostPicks(business);
        if (lost.isEmpty()) {
            return;
        }
        messenger.send(sender, "business.modules.lost-picks", unparsed("business", business.displayName()),
                component("modules", Component.join(JoinConfiguration.commas(true),
                        lost.stream().map(this::name).toList())));
    }
}
