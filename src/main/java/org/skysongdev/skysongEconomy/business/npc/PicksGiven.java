package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Proficiency picks one source gave an NPC in one grant: it may raise {@code count}
 * proficiencies of a skill to a tier.
 * <p>
 * A grant may also promote. {@code promotionCost} more picks then raise a proficiency this grant
 * picked one tier further, up to {@code promoteTo}. SkysongNPCs cannot change a grant's promotion
 * afterwards, so a different promotion means taking the grant back and giving a new one.
 *
 * @param skill         the SkysongStats skill the picks are for
 * @param tier          the tier a pick raises a proficiency to
 * @param count         how many picks the grant holds
 * @param unspent       how many are left to spend
 * @param promoteTo     the highest tier a promotion may reach, {@code null} when the grant cannot promote
 * @param promotionCost picks per promotion; 0 when the grant cannot promote
 */
public record PicksGiven(String skill, String tier, int count, int unspent, @Nullable String promoteTo,
                         int promotionCost) {
    public PicksGiven {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(tier, "tier");
        if ((promoteTo == null) != (promotionCost == 0)) {
            throw new IllegalArgumentException("A grant that promotes needs a tier and a cost of 1 or more: "
                    + promoteTo + ", " + promotionCost);
        }
    }

    /** @return the highest tier a promotion may reach, empty when the grant cannot promote */
    public Optional<String> promotion() {
        return Optional.ofNullable(promoteTo);
    }

    /** @return whether the other grant promotes the same way: to the same tier at the same cost */
    public boolean promotesLike(@Nullable String otherPromoteTo, int otherCost) {
        return Objects.equals(promoteTo, otherPromoteTo) && promotionCost == otherCost;
    }
}
