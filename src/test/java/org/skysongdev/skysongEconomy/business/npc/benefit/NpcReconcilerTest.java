package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.npc.FakeSkysongNpcs;
import org.skysongdev.skysongEconomy.business.npc.NpcPending;
import org.skysongdev.skysongEconomy.business.npc.SpawnPoint;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcReconcilerTest {
    private static final String SOURCE = "economy:skill:martial";
    private static final String PICKS = SOURCE + ":picks";
    private static final Set<String> ANY_STAT = Set.of();

    private final FakeSkysongNpcs skysongNpcs = new FakeSkysongNpcs();
    private final TierOrder tiers = new TierOrder(skysongNpcs.tierIds());
    private final NpcReconciler reconciler = new NpcReconciler(skysongNpcs, tiers);
    private UUID npc;
    private NpcApplied applied = NpcApplied.NONE;

    @BeforeEach
    void createNpc() {
        UUID player = UUID.randomUUID();
        npc = skysongNpcs.create("guard", new CharacterRef(player, "Aldric"), new SpawnPoint.InFrontOf(player), 3)
                .npc().orElseThrow();
        skysongNpcs.completeSetup(npc);
    }

    /** Reconciles the NPC with what the martial source owes it, remembering the sheet changes like the service does. */
    private Changes reconcile(Entitlement owed) {
        NpcReconciler.Result result = reconciler.reconcile(npc, Map.of(SOURCE, owed), applied);
        applied = result.applied();
        return result.changes();
    }

    private static Entitlement owed(int points, Map<PickKind, Integer> picks) {
        return new Entitlement(Map.of(ANY_STAT, points), Map.of(), picks, Map.of(), Set.of(), Map.of());
    }

    private static Entitlement sheet(Map<String, Integer> stats, Map<String, String> proficiencies) {
        return new Entitlement(Map.of(), stats, Map.of(), proficiencies, Set.of(), Map.of());
    }

    private static Entitlement abilities(Set<String> fixed, Map<List<String>, Integer> choices) {
        return new Entitlement(Map.of(), Map.of(), Map.of(), Map.of(), fixed, choices);
    }

    @Test
    void aNewNpcIsGivenEverythingItIsOwed() {
        Changes changes = reconcile(owed(4, Map.of(PickKind.plain("martial", "proficient"), 3)));

        assertEquals(new NpcPending(4, 3, 0), skysongNpcs.pending(npc));
        assertEquals(4, changes.pointsAdded());
        assertEquals(3, changes.picksAdded());
        assertTrue(changes.leavesChoices());
        assertFalse(changes.choicesReset());
    }

    @Test
    void runningItAgainChangesNothing() {
        Entitlement owed = owed(4, Map.of(PickKind.plain("martial", "proficient"), 3));
        reconcile(owed);

        Changes again = reconcile(owed);

        assertFalse(again.any());
        assertEquals(new NpcPending(4, 3, 0), skysongNpcs.pending(npc));
    }

    @Test
    void whenTheBusinessGrowsOnlyTheDifferenceIsGivenAndSpentChoicesStay() {
        PickKind kind = PickKind.plain("martial", "proficient");
        reconcile(owed(4, Map.of(kind, 3)));
        skysongNpcs.spendPoints(npc, SOURCE, 4);
        skysongNpcs.spendPick(npc, PICKS, "swords");

        Changes changes = reconcile(owed(5, Map.of(kind, 4)));

        assertEquals(1, changes.pointsAdded());
        assertEquals(1, changes.picksAdded());
        assertFalse(changes.choicesReset());
        assertEquals(new NpcPending(1, 3, 0), skysongNpcs.pending(npc), "the spent points and the spent pick stay spent");
        assertEquals(Optional.of("proficient"), skysongNpcs.proficiencyTier(npc, "swords"));
    }

    @Test
    void whenTheBusinessShrinksEverythingIsGivenBackAndOnlyWhatIsOwedRemains() {
        PickKind kind = PickKind.plain("martial", "proficient");
        reconcile(owed(5, Map.of(kind, 4)));
        skysongNpcs.spendPoints(npc, SOURCE, 5);
        skysongNpcs.spendPick(npc, PICKS, "swords");

        Changes changes = reconcile(owed(4, Map.of(kind, 3)));

        assertTrue(changes.choicesReset());
        assertTrue(changes.leavesChoices());
        assertEquals(new NpcPending(4, 3, 0), skysongNpcs.pending(npc), "all unspent again, at the lower amounts");
        assertEquals(Optional.of("untrained"), skysongNpcs.proficiencyTier(npc, "swords"));
    }

    @Test
    void whatAnotherSourceGaveIsNeverTouched() {
        skysongNpcs.grantPoints(npc, "template", 2, List.of());
        skysongNpcs.spendPoints(npc, "template", 2);
        skysongNpcs.grantAbility(npc, "haggler", "staff");
        reconcile(owed(5, Map.of()));

        reconcile(Entitlement.NONE);

        assertEquals(NpcPending.NONE, skysongNpcs.pending(npc));
        assertEquals(2, skysongNpcs.points(npc, "template").get(0).spent());
        assertTrue(skysongNpcs.hasAbility(npc, "haggler"));
    }

    @Test
    void whenAdeptOpensOnlyThePicksAreGivenBackToPickAgain() {
        reconcile(owed(4, Map.of(PickKind.plain("martial", "proficient"), 3)));
        skysongNpcs.spendPoints(npc, SOURCE, 4);
        skysongNpcs.spendPick(npc, PICKS, "swords");

        Changes changes = reconcile(owed(4, Map.of(new PickKind("martial", "proficient", "adept", 1), 3)));

        assertTrue(changes.picksReset());
        assertFalse(changes.choicesReset());
        assertEquals(new NpcPending(0, 3, 0), skysongNpcs.pending(npc), "points stay spent, picks are open again");
        skysongNpcs.spendPick(npc, PICKS, "swords");
        assertTrue(skysongNpcs.promote(npc, PICKS, "swords"), "two picks on one proficiency now reach adept");
        assertEquals(Optional.of("adept"), skysongNpcs.proficiencyTier(npc, "swords"));
    }

    @Test
    void fixedAbilitiesAreGrantedAndRevokedWithoutResettingAnything() {
        reconcile(abilities(Set.of("keen-eye"), Map.of()));
        assertTrue(skysongNpcs.hasAbility(npc, "keen-eye"));

        Changes changes = reconcile(abilities(Set.of("pack-mule"), Map.of()));

        assertEquals(List.of("pack-mule"), changes.abilitiesGranted());
        assertEquals(List.of("keen-eye"), changes.abilitiesRevoked());
        assertFalse(changes.leavesChoices());
        assertFalse(skysongNpcs.hasAbility(npc, "keen-eye"));
        assertTrue(skysongNpcs.hasAbility(npc, "pack-mule"));
    }

    @Test
    void anAbilityAnotherSourceAlsoGaveStaysWhenTheBusinessLetsGo() {
        skysongNpcs.grantAbility(npc, "keen-eye", "staff");
        reconcile(abilities(Set.of("keen-eye"), Map.of()));

        reconcile(Entitlement.NONE);

        assertTrue(skysongNpcs.hasAbility(npc, "keen-eye"));
    }

    @Test
    void abilityChoicesGrowAndAreGivenBackWhenTheyShrink() {
        List<String> options = List.of("pack-mule", "haggler");
        reconcile(abilities(Set.of(), Map.of(options, 1)));
        skysongNpcs.chooseAbility(npc, SOURCE, "haggler");

        Changes grown = reconcile(abilities(Set.of(), Map.of(options, 2)));

        assertEquals(1, grown.abilityChoicesAdded());
        assertTrue(skysongNpcs.hasAbility(npc, "haggler"), "the chosen ability stays");
        assertEquals(new NpcPending(0, 0, 1), skysongNpcs.pending(npc));

        Changes shrunk = reconcile(abilities(Set.of(), Map.of()));

        assertTrue(shrunk.choicesReset());
        assertFalse(skysongNpcs.hasAbility(npc, "haggler"));
        assertEquals(NpcPending.NONE, skysongNpcs.pending(npc));
    }

    @Test
    void bakedStatsAreAddedChangedAndRemoved() {
        Changes added = reconcile(sheet(Map.of("wisdom", 1), Map.of()));
        assertEquals(1, added.statsChanged());
        assertEquals(List.of(new FakeSkysongNpcs.Modifier("wisdom", 1)), List.copyOf(skysongNpcs.modifiers(npc).values()));

        assertFalse(reconcile(sheet(Map.of("wisdom", 1), Map.of())).any());

        reconcile(sheet(Map.of("wisdom", 3), Map.of()));
        assertEquals(List.of(new FakeSkysongNpcs.Modifier("wisdom", 3)), List.copyOf(skysongNpcs.modifiers(npc).values()));

        reconcile(Entitlement.NONE);
        assertTrue(skysongNpcs.modifiers(npc).isEmpty());
        assertTrue(applied.isEmpty());
    }

    @Test
    void aBakedStatSomeoneRemovedIsPutBack() {
        reconcile(sheet(Map.of("wisdom", 1), Map.of()));
        skysongNpcs.removeModifier(npc, applied.stats().get("wisdom").modifierId());

        Changes changes = reconcile(sheet(Map.of("wisdom", 1), Map.of()));

        assertEquals(1, changes.statsChanged());
        assertEquals(1, skysongNpcs.modifiers(npc).size());
    }

    @Test
    void aModifierStaffAddedIsLeftAlone() {
        String staffs = skysongNpcs.addModifier(npc, "wisdom", 5).orElseThrow();
        reconcile(sheet(Map.of("wisdom", 1), Map.of()));

        reconcile(Entitlement.NONE);

        assertTrue(skysongNpcs.hasModifier(npc, staffs));
        assertEquals(1, skysongNpcs.modifiers(npc).size());
    }

    @Test
    void fixedProficienciesAreRaisedAndPutBack() {
        reconcile(sheet(Map.of(), Map.of("perception", "proficient")));
        assertEquals(Optional.of("proficient"), skysongNpcs.proficiencyTier(npc, "perception"));

        reconcile(sheet(Map.of(), Map.of("perception", "adept")));
        assertEquals(Optional.of("adept"), skysongNpcs.proficiencyTier(npc, "perception"));

        reconcile(sheet(Map.of(), Map.of("perception", "proficient")));
        assertEquals(Optional.of("proficient"), skysongNpcs.proficiencyTier(npc, "perception"));

        Changes removed = reconcile(Entitlement.NONE);
        assertEquals(1, removed.proficienciesChanged());
        assertEquals(Optional.of("untrained"), skysongNpcs.proficiencyTier(npc, "perception"));
        assertTrue(applied.isEmpty());
    }

    @Test
    void aProficiencyThatIsAlreadyHigherIsNotLowered() {
        skysongNpcs.setProficiencyTier(npc, "perception", "master");

        Changes changes = reconcile(sheet(Map.of(), Map.of("perception", "adept")));

        assertFalse(changes.any());
        assertEquals(Optional.of("master"), skysongNpcs.proficiencyTier(npc, "perception"));
        assertTrue(applied.isEmpty(), "nothing to undo later");
    }

    @Test
    void aProficiencySomeoneRaisedFurtherIsNotPutBack() {
        reconcile(sheet(Map.of(), Map.of("perception", "proficient")));
        skysongNpcs.setProficiencyTier(npc, "perception", "master");

        reconcile(Entitlement.NONE);

        assertEquals(Optional.of("master"), skysongNpcs.proficiencyTier(npc, "perception"));
    }

    @Test
    void twoSourcesAddUpOnTheSheetAndKeepTheirOwnChoices() {
        String module = "economy:module:kennel";
        NpcReconciler.Result result = reconciler.reconcile(npc, Map.of(
                SOURCE, new Entitlement(Map.of(ANY_STAT, 2), Map.of("wisdom", 1), Map.of(), Map.of("perception", "proficient"),
                        Set.of(), Map.of()),
                module, new Entitlement(Map.of(ANY_STAT, 1), Map.of("wisdom", 2), Map.of(), Map.of("perception", "adept"),
                        Set.of(), Map.of())), NpcApplied.NONE);

        assertEquals(3, result.changes().pointsAdded());
        assertEquals(2, skysongNpcs.points(npc, SOURCE).get(0).amount());
        assertEquals(1, skysongNpcs.points(npc, module).get(0).amount());
        assertEquals(List.of(new FakeSkysongNpcs.Modifier("wisdom", 3)), List.copyOf(skysongNpcs.modifiers(npc).values()));
        assertEquals(Optional.of("adept"), skysongNpcs.proficiencyTier(npc, "perception"));
    }
}
