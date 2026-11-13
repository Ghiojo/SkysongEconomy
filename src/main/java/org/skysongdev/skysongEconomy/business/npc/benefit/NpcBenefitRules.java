package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;

import java.util.Map;

/**
 * Something that gives a business's NPCs more than their own slot does: picked modules with an
 * {@code npc-benefit} effect.
 */
@FunctionalInterface
public interface NpcBenefitRules {
    /**
     * @param tiers the proficiency tiers, to tell when picks may promote
     * @return what each of its sources owes the NPC right now, by the source's name in
     *         SkysongNPCs. A source that could have given the NPC something earlier and owes it
     *         nothing now must be listed with {@link Entitlement#NONE}, so its gifts are taken back.
     */
    Map<String, Entitlement> benefits(Business business, BusinessNpc npc, TierOrder tiers);
}
