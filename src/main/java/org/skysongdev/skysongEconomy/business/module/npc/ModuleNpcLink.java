package org.skysongdev.skysongEconomy.business.module.npc;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;

/**
 * Keeps a business's NPCs in line with its modules as either changes:
 * <ul>
 *   <li>a picked module gives its benefits to the NPCs that count at once;</li>
 *   <li>a removed module's own NPCs are removed with its slots, what it gave other NPCs is
 *       taken back, and the choices made for it are forgotten;</li>
 *   <li>a removed NPC frees the shares it was chosen for.</li>
 * </ul>
 */
public final class ModuleNpcLink implements BusinessModuleEvents, BusinessNpcEvents {
    private final BusinessNpcs npcs;
    private final NpcProgression progression;
    private final ModuleTargets targets;

    public ModuleNpcLink(BusinessNpcs npcs, NpcProgression progression, ModuleTargets targets) {
        this.npcs = npcs;
        this.progression = progression;
        this.targets = targets;
    }

    @Override
    public void picked(Business business, ModulePick pick) {
        progression.refresh(business);
    }

    @Override
    public void removed(Business business, ModulePick pick, BusinessModuleEvents.RemovalCause cause) {
        if (cause == BusinessModuleEvents.RemovalCause.BUSINESS_DELETED) {
            targets.businessDeleted(business);
            return;
        }
        targets.pickRemoved(pick);
        npcs.refresh(business);
        progression.refresh(business);
    }

    @Override
    public void created(Business business, BusinessNpc npc) {
    }

    @Override
    public void removed(Business business, BusinessNpc npc, BusinessNpcEvents.RemovalCause cause) {
        targets.npcRemoved(npc.npc());
    }
}
