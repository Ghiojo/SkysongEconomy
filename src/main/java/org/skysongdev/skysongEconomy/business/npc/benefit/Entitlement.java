package org.skysongdev.skysongEconomy.business.npc.benefit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What one giver (a skill's progression, later a module) owes an NPC right now, as plain numbers
 * and ids. Entries with nothing in them are left out.
 *
 * @param points         stat points to assign, by the stats they may go on (empty set: the NPC's usual stats)
 * @param stats          baked stat bonuses, by stat id
 * @param picks          proficiency picks, by kind
 * @param proficiencies  fixed proficiencies: the tier each is raised to at least, by proficiency id
 * @param abilities      fixed abilities
 * @param abilityChoices how many abilities the owner chooses, by the list to choose from
 */
public record Entitlement(Map<Set<String>, Integer> points, Map<String, Integer> stats, Map<PickKind, Integer> picks,
                          Map<String, String> proficiencies, Set<String> abilities,
                          Map<List<String>, Integer> abilityChoices) {
    /** Nothing at all. */
    public static final Entitlement NONE = new Entitlement(Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Map.of());

    public Entitlement {
        points = positive(points);
        stats = positive(stats);
        picks = positive(picks);
        proficiencies = Collections.unmodifiableMap(new LinkedHashMap<>(proficiencies));
        abilities = Collections.unmodifiableSet(new LinkedHashSet<>(abilities));
        abilityChoices = positive(abilityChoices);
    }

    /**
     * @param tiers decides which fixed proficiency tier wins when both name one
     * @return this and the other together: amounts add up, a proficiency keeps its higher tier
     */
    public Entitlement plus(Entitlement other, TierOrder tiers) {
        Map<String, String> allProficiencies = new LinkedHashMap<>(proficiencies);
        other.proficiencies.forEach((proficiency, tier) -> allProficiencies.merge(proficiency, tier, tiers::higher));
        Set<String> allAbilities = new LinkedHashSet<>(abilities);
        allAbilities.addAll(other.abilities);
        return new Entitlement(sum(points, other.points), sum(stats, other.stats), sum(picks, other.picks),
                allProficiencies, allAbilities, sum(abilityChoices, other.abilityChoices));
    }

    /** @return whether there is nothing in it */
    public boolean isEmpty() {
        return points.isEmpty() && stats.isEmpty() && picks.isEmpty() && proficiencies.isEmpty() && abilities.isEmpty()
                && abilityChoices.isEmpty();
    }

    /** @return every stat point to assign, whatever stats they may go on */
    public int totalPoints() {
        return points.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** @return every proficiency pick, of every kind */
    public int totalPicks() {
        return picks.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** @return every ability the owner chooses, from every list */
    public int totalAbilityChoices() {
        return abilityChoices.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** @return the kinds of picks in a stable order: by skill, then tier */
    public List<PickKind> pickKinds() {
        List<PickKind> kinds = new ArrayList<>(picks.keySet());
        kinds.sort((first, second) -> (first.skill() + ' ' + first.tier()).compareTo(second.skill() + ' ' + second.tier()));
        return kinds;
    }

    private static <K> Map<K, Integer> sum(Map<K, Integer> first, Map<K, Integer> second) {
        Map<K, Integer> total = new LinkedHashMap<>(first);
        second.forEach((key, amount) -> total.merge(key, amount, Integer::sum));
        return total;
    }

    private static <K> Map<K, Integer> positive(Map<K, Integer> amounts) {
        Map<K, Integer> kept = new LinkedHashMap<>();
        amounts.forEach((key, amount) -> {
            if (amount > 0) {
                kept.put(key, amount);
            }
        });
        return Collections.unmodifiableMap(kept);
    }
}
