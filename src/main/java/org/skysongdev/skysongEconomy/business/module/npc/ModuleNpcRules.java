package org.skysongdev.skysongEconomy.business.module.npc;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcSlotRules;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitSources;
import org.skysongdev.skysongEconomy.business.npc.benefit.Entitlement;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcBenefitRules;
import org.skysongdev.skysongEconomy.business.npc.benefit.TierOrder;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * What picked modules mean for NPCs, in the terms the NPC services ask in.
 * <ul>
 *   <li>A module with an {@code npc-slots} effect is a slot source of its own,
 *       {@code module:<id>}, with its own template and progression. Picked twice, it gives its
 *       slots twice.</li>
 *   <li>A module with {@code npc-benefit} effects owes NPCs a benefit under the source
 *       {@code economy:module:<id>}: every NPC that counts for a target of {@code all}, and the
 *       NPCs the owner chose for a numbered target.</li>
 * </ul>
 * Amounts are worked out at the level of the skill the module was picked for. A module whose
 * file is gone says nothing, so what it gave stays as it is.
 */
public final class ModuleNpcRules implements NpcSlotRules, NpcBenefitRules {
    private final BusinessModules modules;
    private final ModuleTargets targets;

    public ModuleNpcRules(BusinessModules modules, ModuleTargets targets) {
        this.modules = modules;
        this.targets = targets;
    }

    @Override
    public Map<SlotSource, Integer> capacities(Business business) {
        Map<SlotSource, Integer> capacities = new LinkedHashMap<>();
        for (ModulePick pick : modules.of(business)) {
            slots(pick.moduleId()).ifPresent(slots -> capacities.merge(SlotSource.module(pick.moduleId()),
                    slots.amount().at(BusinessScale.of(business, pick.skill())), Integer::sum));
        }
        return capacities;
    }

    @Override
    public OptionalInt capacity(Business business, SlotSource source) {
        if (!source.isModule() || slots(source.id()).isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(capacities(business).getOrDefault(source, 0));
    }

    @Override
    public Optional<String> template(SlotSource source) {
        return source.isModule() ? slots(source.id()).map(ModuleEffect.NpcSlots::template) : Optional.empty();
    }

    @Override
    public Optional<Entitlement> entitlement(Business business, SlotSource source, TierOrder tiers) {
        if (!source.isModule()) {
            return Optional.empty();
        }
        return slots(source.id()).map(slots -> modules.of(business).stream()
                .filter(pick -> pick.moduleId().equals(source.id())).findFirst()
                .map(pick -> slots.progression().at(BusinessScale.of(business, pick.skill()), tiers))
                .orElse(Entitlement.NONE));
    }

    @Override
    public Map<String, Entitlement> benefits(Business business, BusinessNpc npc, TierOrder tiers) {
        Map<String, Entitlement> bySource = new LinkedHashMap<>();
        for (Module module : modules.modules()) {
            if (!module.effects(ModuleEffect.NpcBenefit.class).isEmpty()) {
                bySource.put(source(module.id()), Entitlement.NONE);
            }
        }
        for (ModulePick pick : modules.of(business)) {
            Optional<Module> module = modules.module(pick.moduleId());
            if (module.isEmpty()) {
                continue;
            }
            List<ModuleEffect> effects = module.get().effects();
            for (int i = 0; i < effects.size(); i++) {
                if (effects.get(i) instanceof ModuleEffect.NpcBenefit benefit && benefit.target().counts(npc)
                        && (benefit.target().isAll() || targets.isChosen(pick, i, npc.npc()))) {
                    Entitlement owed = benefit.benefit().at(BusinessScale.of(business, pick.skill()), tiers);
                    bySource.merge(source(pick.moduleId()), owed, (a, b) -> a.plus(b, tiers));
                }
            }
        }
        return bySource;
    }

    /** @return the name in SkysongNPCs of what a module gives NPCs: {@code economy:module:<id>} */
    public static String source(String moduleId) {
        return BenefitSources.of(SlotSource.module(moduleId));
    }

    /** @return the module a source in SkysongNPCs belongs to, empty when it is not a module's */
    public static Optional<String> moduleOf(String source) {
        String prefix = source("x");
        prefix = prefix.substring(0, prefix.length() - 1);
        return source.startsWith(prefix) && source.length() > prefix.length()
                ? Optional.of(source.substring(prefix.length())) : Optional.empty();
    }

    private Optional<ModuleEffect.NpcSlots> slots(String moduleId) {
        return modules.module(moduleId).flatMap(module -> module.effects(ModuleEffect.NpcSlots.class).stream().findFirst());
    }
}
