package org.skysongdev.skysongEconomy.business.npc;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What an NPC was given, and the means to give and take it back. Main thread only.
 * <p>
 * Stat points, proficiency picks, abilities and ability choices live in SkysongNPCs and are
 * tracked per <em>source</em>: a short text without spaces, at most {@value #MAX_SOURCE_LENGTH}
 * characters, naming who gave them. Every method that takes a source only sees and changes what
 * that source gave. Baked stats and fixed proficiencies go on the NPC's SkysongStats sheet, which
 * has no sources, so the caller remembers what it applied.
 * <p>
 * Ids are SkysongStats and SkysongNPCs ids. The {@code is...} methods let config be checked on
 * load instead of failing when an NPC is created.
 */
public interface NpcSheetGateway {
    /** The longest source SkysongNPCs accepts. */
    int MAX_SOURCE_LENGTH = 64;

    /** @return the choices the NPC still has to make, from every source */
    NpcPending pending(UUID npc);

    // Stat points the owner assigns

    /** @return the source's point grants on that NPC, oldest first; empty when it gave none */
    List<PointsGiven> points(UUID npc, String source);

    /**
     * Gives the NPC stat points to assign.
     *
     * @param amount   1 or more
     * @param eligible the stats they may go on; empty for the NPC's default eligible stats
     */
    void grantPoints(UUID npc, String source, int amount, Collection<String> eligible);

    /**
     * Takes back points the source gave that are not assigned yet, newest grant first.
     *
     * @param max how many at most
     * @return how many were taken
     */
    int takeUnspentPoints(UUID npc, String source, int max);

    // Proficiency picks the owner makes

    /** @return the source's pick grants on that NPC, oldest first; empty when it gave none */
    List<PicksGiven> picks(UUID npc, String source);

    /**
     * Gives the NPC proficiency picks. Picks with the same source, skill, tier and promotion as
     * a grant it already has are added to that grant.
     *
     * @param count         1 or more
     * @param promoteTo     the highest tier a promotion may reach, {@code null} for picks that cannot promote
     * @param promotionCost picks per promotion, 1 or more; ignored without {@code promoteTo}
     */
    void grantPicks(UUID npc, String source, String skill, String tier, int count, @Nullable String promoteTo,
                    int promotionCost);

    /**
     * Takes back picks the source gave that are not spent yet, newest grant first.
     *
     * @param max how many at most
     * @return how many were taken
     */
    int takeUnspentPicks(UUID npc, String source, int max);

    /**
     * Undoes every choice the source's grants paid for: assigned stat points, picked and promoted
     * proficiencies and chosen abilities become unspent again. The grants themselves stay; take
     * back what is no longer owed afterwards. Other sources' choices are not touched.
     *
     * @return how many choices were given back
     */
    int respec(UUID npc, String source);

    // Abilities

    /** @return whether that source gave the NPC the ability and still holds it */
    boolean holdsAbility(UUID npc, String ability, String source);

    /** @return the abilities that source gave the NPC and still holds; abilities the NPC chose are not among them */
    Set<String> abilitiesHeldBy(UUID npc, String source);

    /**
     * Gives the NPC an ability on behalf of the source. Two sources may hold the same ability;
     * the NPC keeps it until the last one lets go.
     *
     * @return false when the source held it already
     */
    boolean grantAbility(UUID npc, String ability, String source);

    /**
     * Takes the source's hold on an ability away.
     *
     * @return false when the source did not hold it
     */
    boolean revokeAbility(UUID npc, String ability, String source);

    /** @return the source's ability choices on that NPC, oldest first; empty when it gave none */
    List<AbilityChoiceGiven> abilityChoices(UUID npc, String source);

    /**
     * Lets the NPC choose abilities from a list. A choice with the same source and options as one
     * it already has grows by {@code count}.
     *
     * @param options the ability ids to choose from
     * @param count   how many it may choose, 1 or more
     */
    void grantAbilityChoice(UUID npc, String source, List<String> options, int count);

    /**
     * Takes back the choices the source gave that are not made yet. Chosen abilities stay.
     *
     * @return how many were taken
     */
    int takeUnspentAbilityChoices(UUID npc, String source);

    // Baked stats: modifiers on the NPC's SkysongStats sheet

    /**
     * Adds a permanent bonus or penalty to one of the NPC's stats.
     *
     * @param stat   the stat's id
     * @param amount the bonus; negative for a penalty
     * @return the new modifier's id, to remove it with; empty when the NPC has no sheet or the
     *         stat does not exist
     */
    Optional<String> addModifier(UUID npc, String stat, int amount);

    /** @return false when the NPC has no modifier with that id */
    boolean removeModifier(UUID npc, String modifierId);

    /** @return whether the NPC's sheet still has the modifier with that id */
    boolean hasModifier(UUID npc, String modifierId);

    // Fixed proficiencies: tiers on the NPC's SkysongStats sheet

    /** @return the tier id the NPC has in that proficiency; empty when it has no sheet or there is no such proficiency */
    Optional<String> proficiencyTier(UUID npc, String proficiency);

    /** @return false when the NPC has no sheet, or the proficiency or tier does not exist */
    boolean setProficiencyTier(UUID npc, String proficiency, String tier);

    // What exists, for checking config

    /** @return every proficiency tier id, lowest first (untrained, proficient, adept, master) */
    List<String> tierIds();

    /** @return whether SkysongStats has a stat with that id */
    boolean isStat(String stat);

    /** @return whether SkysongStats has a proficiency with that id */
    boolean isProficiency(String proficiency);

    /** @return whether SkysongNPCs' abilities.yml defines that ability */
    boolean isAbility(String ability);
}
