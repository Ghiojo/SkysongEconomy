package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;
import org.skysongdev.skysongEconomy.business.scale.Steps;

import java.util.Objects;

/**
 * What an NPC receives as its business grows: a benefit it has from the start, and more benefits
 * at steps of the business's level or tier. Every step reached counts, on top of the ones below.
 *
 * @param always what the NPC has from the start; its amounts may still follow level and tier
 * @param steps  what is added at each step ({@code at:} in config), {@code null} for none
 */
public record Progression(Benefit always, @Nullable Steps<Benefit> steps) {
    /** An NPC that receives nothing. */
    public static final Progression NONE = new Progression(Benefit.NONE, null);

    public Progression {
        Objects.requireNonNull(always, "always");
    }

    /** @return whether no NPC ever receives anything from it */
    public boolean isEmpty() {
        return always.isEmpty() && steps == null;
    }

    /**
     * @param scale the business's level and tier
     * @param tiers the proficiency tiers
     * @return everything an NPC is owed at that scale
     */
    public Entitlement at(BusinessScale scale, TierOrder tiers) {
        Entitlement total = always.at(scale, tiers);
        if (steps != null) {
            for (Benefit step : steps.reached(scale)) {
                total = total.plus(step.at(scale, tiers), tiers);
            }
        }
        return total;
    }
}
