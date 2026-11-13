package org.skysongdev.skysongEconomy.commands.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.Hire;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModulePickGroup;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleWorkerSlots;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleShare;
import org.skysongdev.skysongEconomy.business.module.npc.ModuleTargets;
import org.skysongdev.skysongEconomy.character.SkillCatalog;

import java.util.List;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

/** What the module commands share: the module services and how modules are written in chat. */
final class ModuleCommandText implements ChangeReport {
    private final BusinessCommandSupport support;
    private final BusinessModules modules;
    private final ModuleWorkerSlots workers;
    private final SkillCatalog skills;
    /** Which NPCs were chosen for module benefits; {@code null} until the business NPCs run. */
    private @Nullable ModuleTargets targets;

    /**
     * @param modules the modules each business picked
     * @param workers the workers a business loses with a module
     * @param skills  skill names
     */
    ModuleCommandText(BusinessCommandSupport support, BusinessModules modules, ModuleWorkerSlots workers, SkillCatalog skills) {
        this.support = support;
        this.modules = modules;
        this.workers = workers;
        this.skills = skills;
    }

    BusinessModules modules() {
        return modules;
    }

    /** Lets the lists say how many NPCs are still to choose for each module. */
    void enableNpcs(ModuleTargets chosen) {
        this.targets = chosen;
    }

    /** @return the module's name with its formatting, or its id when its file is gone */
    Component name(ModulePick pick) {
        return modules.module(pick.moduleId()).map(Module::name).map(support.messenger::renderText)
                .orElseGet(() -> Component.text(pick.moduleId()));
    }

    /** Lists the business's modules skill by skill, numbered within each skill; silent when it has no module picks. */
    void list(CommandSender sender, Business business) {
        for (ModulePickGroup group : modules.groups(business)) {
            support.messenger.send(sender, "business.modules.list-group", parsed("skill", skills.displayName(group.skill())),
                    unparsed("id", group.skill()), unparsed("picked", String.valueOf(group.picks().size())),
                    unparsed("max", String.valueOf(Math.max(group.total(), group.picks().size()))));
            for (int i = 0; i < group.picks().size(); i++) {
                ModulePick pick = group.picks().get(i);
                support.messenger.send(sender, "business.modules.list-module", unparsed("number", String.valueOf(i + 1)),
                        component("module", name(pick)), unparsed("id", pick.moduleId()));
                int open = open(business, pick);
                if (open > 0) {
                    support.messenger.send(sender, "business.modules.list-open", unparsed("count", String.valueOf(open)));
                }
            }
        }
    }

    private int open(Business business, ModulePick pick) {
        return targets == null ? 0 : targets.shares(business, pick).stream().mapToInt(ModuleShare::open).sum();
    }

    /** Tells the sender which modules and workers the business just lost, if any. */
    @Override
    public void report(CommandSender sender, Business business) {
        List<ModulePick> lost = modules.takeLostPicks(business);
        if (!lost.isEmpty()) {
            support.messenger.send(sender, "business.modules.lost-picks", unparsed("business", business.displayName()),
                    component("modules", Component.join(JoinConfiguration.commas(true), lost.stream().map(this::name).toList())));
        }
        List<Hire> fired = workers.takeLostWorkers(business);
        if (!fired.isEmpty()) {
            support.messenger.send(sender, "business.modules.lost-workers", unparsed("business", business.displayName()),
                    unparsed("workers", fired.stream().map(hire -> support.describe(hire.character()))
                            .collect(Collectors.joining(", "))));
        }
    }
}
