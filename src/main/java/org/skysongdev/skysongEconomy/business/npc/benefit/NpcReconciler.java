package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.skysongdev.skysongEconomy.business.npc.AbilityChoiceGiven;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.npc.PicksGiven;
import org.skysongdev.skysongEconomy.business.npc.PointsGiven;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Brings one NPC in line with what it is owed. It never applies a benefit as a one-off: it reads
 * what each source already gave the NPC, compares that with the entitlement, and applies only
 * the difference. Running it twice in a row changes nothing the second time.
 * <ul>
 *   <li><b>More than before:</b> the extra points, picks, choices and abilities are given.</li>
 *   <li><b>Less than before:</b> everything that source gave is given back to choose again, what
 *       is no longer owed is taken away, and the rest is granted anew.</li>
 *   <li><b>Picks that promote differently</b> (adept opened, or closed): only the picks are
 *       given back and granted anew, since SkysongNPCs cannot change a grant's promotion.</li>
 *   <li><b>Baked stats and fixed proficiencies</b> are set on the sheet, and
 *       {@link NpcApplied} remembers them so exactly those are undone later.</li>
 * </ul>
 * Main thread only.
 */
public final class NpcReconciler {
    private final NpcSheetGateway sheets;
    private final TierOrder tiers;

    public NpcReconciler(NpcSheetGateway sheets, TierOrder tiers) {
        this.sheets = sheets;
        this.tiers = tiers;
    }

    /**
     * What a reconciliation did.
     *
     * @param changes what changed on the NPC
     * @param applied what is now changed on its sheet, to save
     */
    public record Result(Changes changes, NpcApplied applied) {
    }

    /**
     * @param npc      the NPC
     * @param bySource what each source owes it now; a source that owes nothing any more must
     *                 still be listed, with {@link Entitlement#NONE}, to have its gifts taken back
     * @param applied  what was changed on its sheet so far
     * @return what changed, and what is changed on the sheet now
     */
    public Result reconcile(UUID npc, Map<String, Entitlement> bySource, NpcApplied applied) {
        Tally tally = new Tally();
        Entitlement total = Entitlement.NONE;
        for (Map.Entry<String, Entitlement> entry : bySource.entrySet()) {
            reconcileSource(npc, entry.getKey(), entry.getValue(), tally);
            total = total.plus(entry.getValue(), tiers);
        }
        NpcApplied nowApplied = new NpcApplied(reconcileStats(npc, total.stats(), applied.stats(), tally),
                reconcileProficiencies(npc, total.proficiencies(), applied.proficiencies(), tally));
        return new Result(tally.changes(), nowApplied);
    }

    /** Counts what one reconciliation changes. */
    private static final class Tally {
        private int points;
        private int picks;
        private int abilityChoices;
        private boolean choicesReset;
        private boolean picksReset;
        private final List<String> granted = new ArrayList<>();
        private final List<String> revoked = new ArrayList<>();
        private int stats;
        private int proficiencies;

        private Changes changes() {
            return new Changes(points, picks, abilityChoices, choicesReset, picksReset, granted, revoked, stats, proficiencies);
        }
    }

    // What SkysongNPCs tracks by source: points, picks, ability choices, fixed abilities

    private void reconcileSource(UUID npc, String source, Entitlement owed, Tally tally) {
        String picksSource = BenefitSources.picks(source);
        Map<Set<String>, Integer> points = pointsGiven(npc, source);
        Map<List<String>, Integer> choices = choicesGiven(npc, source);
        Map<PickKind, Integer> picks = picksGiven(npc, picksSource);

        boolean owedLess = exceeds(points, owed.points()) || exceeds(choices, owed.abilityChoices())
                || exceeds(byBase(picks), byBase(owed.picks()));
        if (owedLess) {
            sheets.respec(npc, source);
            sheets.takeUnspentPoints(npc, source, Integer.MAX_VALUE);
            sheets.takeUnspentAbilityChoices(npc, source);
            points = pointsGiven(npc, source);
            choices = choicesGiven(npc, source);
            tally.choicesReset = true;
        }
        if (owedLess || exceeds(picks, owed.picks())) {
            sheets.respec(npc, picksSource);
            sheets.takeUnspentPicks(npc, picksSource, Integer.MAX_VALUE);
            picks = picksGiven(npc, picksSource);
            tally.picksReset |= !owedLess;
        }

        for (Map.Entry<Set<String>, Integer> entry : owed.points().entrySet()) {
            int missing = entry.getValue() - points.getOrDefault(entry.getKey(), 0);
            if (missing > 0) {
                sheets.grantPoints(npc, source, missing, entry.getKey());
                tally.points += missing;
            }
        }
        for (PickKind kind : owed.pickKinds()) {
            int missing = owed.picks().get(kind) - picks.getOrDefault(kind, 0);
            if (missing > 0) {
                sheets.grantPicks(npc, picksSource, kind.skill(), kind.tier(), missing, kind.promoteTo(), kind.promotionCost());
                tally.picks += missing;
            }
        }
        for (Map.Entry<List<String>, Integer> entry : owed.abilityChoices().entrySet()) {
            int missing = entry.getValue() - choices.getOrDefault(entry.getKey(), 0);
            if (missing > 0) {
                sheets.grantAbilityChoice(npc, source, entry.getKey(), missing);
                tally.abilityChoices += missing;
            }
        }

        Set<String> held = sheets.abilitiesHeldBy(npc, source);
        for (String ability : owed.abilities()) {
            if (!held.contains(ability) && sheets.grantAbility(npc, ability, source)) {
                tally.granted.add(ability);
            }
        }
        for (String ability : held) {
            if (!owed.abilities().contains(ability) && sheets.revokeAbility(npc, ability, source)) {
                tally.revoked.add(ability);
            }
        }
    }

    private Map<Set<String>, Integer> pointsGiven(UUID npc, String source) {
        Map<Set<String>, Integer> given = new LinkedHashMap<>();
        for (PointsGiven grant : sheets.points(npc, source)) {
            given.merge(grant.eligible(), grant.amount(), Integer::sum);
        }
        return given;
    }

    private Map<List<String>, Integer> choicesGiven(UUID npc, String source) {
        Map<List<String>, Integer> given = new LinkedHashMap<>();
        for (AbilityChoiceGiven choice : sheets.abilityChoices(npc, source)) {
            given.merge(choice.options(), choice.count(), Integer::sum);
        }
        return given;
    }

    private Map<PickKind, Integer> picksGiven(UUID npc, String source) {
        Map<PickKind, Integer> given = new LinkedHashMap<>();
        for (PicksGiven grant : sheets.picks(npc, source)) {
            given.merge(new PickKind(grant.skill(), grant.tier(), grant.promoteTo(), grant.promotionCost()), grant.count(),
                    Integer::sum);
        }
        return given;
    }

    /** @return the picks by skill and tier only, whatever their promotion */
    private static Map<PickKind, Integer> byBase(Map<PickKind, Integer> picks) {
        Map<PickKind, Integer> totals = new LinkedHashMap<>();
        picks.forEach((kind, count) -> totals.merge(PickKind.plain(kind.skill(), kind.tier()), count, Integer::sum));
        return totals;
    }

    /** @return whether the NPC was given more of something than it is owed */
    private static <K> boolean exceeds(Map<K, Integer> given, Map<K, Integer> owed) {
        return given.entrySet().stream().anyMatch(entry -> entry.getValue() > owed.getOrDefault(entry.getKey(), 0));
    }

    // What only the sheet knows: baked stats and fixed proficiencies

    private Map<String, NpcApplied.Stat> reconcileStats(UUID npc, Map<String, Integer> owed,
                                                        Map<String, NpcApplied.Stat> applied, Tally tally) {
        Map<String, NpcApplied.Stat> now = new LinkedHashMap<>();
        Set<String> stats = new LinkedHashSet<>(applied.keySet());
        stats.addAll(owed.keySet());
        for (String stat : stats) {
            NpcApplied.Stat was = applied.get(stat);
            int amount = owed.getOrDefault(stat, 0);
            if (was != null && was.amount() == amount && sheets.hasModifier(npc, was.modifierId())) {
                now.put(stat, was);
                continue;
            }
            if (was != null) {
                sheets.removeModifier(npc, was.modifierId());
            }
            Optional<String> added = amount == 0 ? Optional.empty() : sheets.addModifier(npc, stat, amount);
            added.ifPresent(id -> now.put(stat, new NpcApplied.Stat(id, amount)));
            if (was != null || added.isPresent()) {
                tally.stats++;
            }
        }
        return now;
    }

    private Map<String, NpcApplied.Proficiency> reconcileProficiencies(UUID npc, Map<String, String> owed,
                                                                       Map<String, NpcApplied.Proficiency> applied,
                                                                       Tally tally) {
        Map<String, NpcApplied.Proficiency> now = new LinkedHashMap<>();
        Set<String> proficiencies = new LinkedHashSet<>(applied.keySet());
        proficiencies.addAll(owed.keySet());
        for (String proficiency : proficiencies) {
            Optional<String> onSheet = sheets.proficiencyTier(npc, proficiency);
            if (onSheet.isEmpty()) {
                continue;
            }
            String current = onSheet.get();
            NpcApplied.Proficiency was = applied.get(proficiency);
            // If someone else changed the tier since, what is there now is what it goes back to.
            String before = was != null && current.equals(was.applied()) ? was.before() : current;
            String wanted = owed.get(proficiency);
            String target = wanted == null ? before : tiers.higher(before, wanted);
            if (!target.equals(current) && sheets.setProficiencyTier(npc, proficiency, target)) {
                tally.proficiencies++;
            }
            if (wanted != null && tiers.isAbove(wanted, before)) {
                now.put(proficiency, new NpcApplied.Proficiency(before, wanted));
            }
        }
        return now;
    }
}
