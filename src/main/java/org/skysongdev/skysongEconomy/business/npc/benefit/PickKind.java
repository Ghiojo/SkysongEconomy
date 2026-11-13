package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One kind of proficiency pick: picks of the same kind can share a grant in SkysongNPCs.
 *
 * @param skill         the SkysongStats skill the picks are for
 * @param tier          the tier a pick raises a proficiency to
 * @param promoteTo     the highest tier a promotion may reach, {@code null} when picks cannot promote
 * @param promotionCost picks per promotion; 0 when picks cannot promote
 */
public record PickKind(String skill, String tier, @Nullable String promoteTo, int promotionCost) {
    public PickKind {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(tier, "tier");
        if ((promoteTo == null) != (promotionCost == 0)) {
            throw new IllegalArgumentException("Picks that promote need a tier and a cost of 1 or more: "
                    + promoteTo + ", " + promotionCost);
        }
    }

    /** @return picks of that skill and tier that cannot promote */
    public static PickKind plain(String skill, String tier) {
        return new PickKind(skill, tier, null, 0);
    }

    /** @return whether the other kind is for the same skill and tier, whatever its promotion */
    public boolean sameBase(PickKind other) {
        return skill.equals(other.skill) && tier.equals(other.tier);
    }
}
