package org.skysongdev.skysongEconomy.business.module.npc;

import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect;

import java.util.List;
import java.util.UUID;

/**
 * One {@code npc-benefit} effect of a picked module whose target is a number: the owner chooses
 * which NPCs receive it.
 *
 * @param pick   the module pick
 * @param module the module
 * @param effect which of the module's effects, counted from 0 in file order
 * @param benefit the effect itself
 * @param chosen the NPCs chosen so far, in the order they were chosen
 */
public record ModuleShare(ModulePick pick, Module module, int effect, ModuleEffect.NpcBenefit benefit, List<UUID> chosen) {
    public ModuleShare {
        chosen = List.copyOf(chosen);
    }

    /** @return how many NPCs can still be chosen */
    public int open() {
        return Math.max(0, benefit.target().count() - chosen.size());
    }
}
