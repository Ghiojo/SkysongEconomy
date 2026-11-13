package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.scale.Amount;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;
import org.skysongdev.skysongEconomy.business.scale.Steps;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One benefit block, as config writes it: whatever an NPC can receive. Every part is optional.
 * Amounts may follow the business's level and tier, so a benefit only becomes numbers at a
 * {@link BusinessScale}; see {@link #at}.
 *
 * @param statPoints     points the owner assigns, {@code null} for none
 * @param stats          baked stat bonuses, by stat id
 * @param picks          proficiency picks the owner makes
 * @param proficiencies  fixed proficiencies: proficiency id to the tier it is raised to at least
 * @param abilities      fixed ability ids
 * @param abilityChoices abilities the owner chooses, {@code null} for none
 */
public record Benefit(@Nullable StatPoints statPoints, Map<String, Amount> stats, List<ProficiencyPicks> picks,
                      Map<String, String> proficiencies, List<String> abilities,
                      @Nullable AbilityChoices abilityChoices) {
    /** A block with nothing in it. */
    public static final Benefit NONE = new Benefit(null, Map.of(), List.of(), Map.of(), List.of(), null);

    public Benefit {
        stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
        picks = List.copyOf(picks);
        proficiencies = Collections.unmodifiableMap(new LinkedHashMap<>(proficiencies));
        abilities = List.copyOf(abilities);
    }

    /**
     * Stat points the owner assigns.
     *
     * @param amount   how many
     * @param eligible the stats they may go on; empty for the NPC's usual stats
     */
    public record StatPoints(Amount amount, Set<String> eligible) {
        public StatPoints {
            Objects.requireNonNull(amount, "amount");
            eligible = Set.copyOf(eligible);
        }
    }

    /**
     * Proficiency picks the owner makes.
     *
     * @param skill       the SkysongStats skill whose proficiencies can be picked
     * @param amount      how many picks
     * @param tier        the tier a pick raises a proficiency to
     * @param cap         the highest tier the picks may reach as the business grows, {@code null}
     *                    when it never goes above {@code tier}
     * @param promoteCost picks one step up costs; 0 when the picks cannot promote
     */
    public record ProficiencyPicks(String skill, Amount amount, String tier, @Nullable Steps<String> cap, int promoteCost) {
        public ProficiencyPicks {
            Objects.requireNonNull(skill, "skill");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(tier, "tier");
        }

        /** @return the kind of pick this gives at that scale: with promotion only while the cap is above the tier */
        public PickKind kindAt(BusinessScale scale, TierOrder tiers) {
            Optional<String> reached = cap == null ? Optional.empty() : cap.at(scale);
            if (promoteCost > 0 && reached.isPresent() && tiers.isAbove(reached.get(), tier)) {
                return new PickKind(skill, tier, reached.get(), promoteCost);
            }
            return PickKind.plain(skill, tier);
        }
    }

    /**
     * Abilities the owner chooses.
     *
     * @param amount how many
     * @param from   the ability ids to choose from
     */
    public record AbilityChoices(Amount amount, List<String> from) {
        public AbilityChoices {
            Objects.requireNonNull(amount, "amount");
            from = List.copyOf(from);
        }
    }

    /** @return whether the block gives nothing */
    public boolean isEmpty() {
        return statPoints == null && stats.isEmpty() && picks.isEmpty() && proficiencies.isEmpty() && abilities.isEmpty()
                && abilityChoices == null;
    }

    /**
     * @param scale the business's level and tier
     * @param tiers the proficiency tiers, to tell when picks may promote
     * @return what this block gives at that scale
     */
    public Entitlement at(BusinessScale scale, TierOrder tiers) {
        Map<Set<String>, Integer> points = statPoints == null
                ? Map.of() : Map.of(statPoints.eligible(), statPoints.amount().at(scale));
        Map<String, Integer> bakedStats = new LinkedHashMap<>();
        stats.forEach((stat, amount) -> bakedStats.put(stat, amount.at(scale)));
        Map<PickKind, Integer> pickCounts = new LinkedHashMap<>();
        for (ProficiencyPicks pick : picks) {
            pickCounts.merge(pick.kindAt(scale, tiers), pick.amount().at(scale), Integer::sum);
        }
        Map<List<String>, Integer> choices = abilityChoices == null
                ? Map.of() : Map.of(abilityChoices.from(), abilityChoices.amount().at(scale));
        return new Entitlement(points, bakedStats, pickCounts, proficiencies, Set.copyOf(abilities), choices);
    }
}
