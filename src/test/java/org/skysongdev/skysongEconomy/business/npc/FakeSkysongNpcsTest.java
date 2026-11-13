package org.skysongdev.skysongEconomy.business.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins down how the fake behaves, which is how SkysongNPCs' API documents the real thing. The
 * business NPC tests rely on these rules, so they are checked once here.
 */
class FakeSkysongNpcsTest {
    private static final String SKILL = "economy:skill:martial";
    private static final String PICKS = "economy:skill:martial:picks";
    private static final String MODULE = "economy:module:quartermaster";
    private static final SpawnPoint SPOT = new SpawnPoint.NextTo(new BlockPosition("world", 1, 64, 1), null);

    private final CharacterRef owner = new CharacterRef(UUID.nameUUIDFromBytes("Owner".getBytes()), "Owner");
    private FakeSkysongNpcs npcs;
    private UUID npc;

    @BeforeEach
    void setUp() {
        npcs = new FakeSkysongNpcs();
        npc = npcs.create("guard", owner, SPOT, 3).npc().orElseThrow();
    }

    @Test
    void aNewNpcBelongsToItsOwnerAndWaitsForSetup() {
        assertTrue(npcs.exists(npc));
        assertTrue(npcs.inSetup(npc));
        assertEquals(Optional.of(owner), npcs.ownerOf(npc));
        assertEquals(List.of(SPOT), npcs.spawnPoints);

        npcs.completeSetup(npc);
        assertFalse(npcs.inSetup(npc));
    }

    @Test
    void nothingIsCreatedWithoutAFreeSpotOrATemplate() {
        assertEquals(NpcCreation.Outcome.UNKNOWN_TEMPLATE, npcs.create("dragon", owner, SPOT, 3).outcome());
        npcs.freeSpot = false;
        NpcCreation refused = npcs.create("guard", owner, SPOT, 3);

        assertEquals(NpcCreation.Outcome.NO_SPOT, refused.outcome());
        assertFalse(refused.isCreated());
        assertEquals(List.of(npc), npcs.all());
    }

    @Test
    void anNpcCanChangeOwnerAndBeRemoved() {
        assertTrue(npcs.setOwner(npc, null));
        assertEquals(Optional.empty(), npcs.ownerOf(npc), "the server owns it");

        assertTrue(npcs.remove(npc));
        assertFalse(npcs.exists(npc));
        assertFalse(npcs.remove(npc));
        assertFalse(npcs.setOwner(npc, owner));
    }

    @Test
    void onlyUnspentPointsOfTheSameSourceAreTakenBack() {
        npcs.grantPoints(npc, SKILL, 4, Set.of());
        npcs.grantPoints(npc, MODULE, 2, Set.of("strength"));
        npcs.spendPoints(npc, SKILL, 3);

        assertEquals(1, npcs.takeUnspentPoints(npc, SKILL, 10));
        assertEquals(List.of(new PointsGiven(3, 3, Set.of())), npcs.points(npc, SKILL));
        assertEquals(List.of(new PointsGiven(2, 0, Set.of("strength"))), npcs.points(npc, MODULE));
    }

    @Test
    void aRespecUnspendsOneSourcesPointsAndLeavesTheGrant() {
        npcs.grantPoints(npc, SKILL, 4, Set.of());
        npcs.grantPoints(npc, MODULE, 2, Set.of());
        npcs.spendPoints(npc, SKILL, 4);
        npcs.spendPoints(npc, MODULE, 2);

        assertEquals(4, npcs.respec(npc, SKILL));
        assertEquals(4, npcs.points(npc, SKILL).get(0).unspent());
        assertEquals(0, npcs.points(npc, MODULE).get(0).unspent(), "another source's choices stay");
    }

    @Test
    void picksWithTheSamePromotionGrowOneGrant() {
        npcs.grantPicks(npc, PICKS, "martial", "proficient", 3, null, 0);
        npcs.grantPicks(npc, PICKS, "martial", "proficient", 1, null, 0);
        npcs.grantPicks(npc, PICKS, "martial", "proficient", 2, "adept", 1);

        assertEquals(List.of(new PicksGiven("martial", "proficient", 4, 4, null, 0),
                new PicksGiven("martial", "proficient", 2, 2, "adept", 1)), npcs.picks(npc, PICKS));
    }

    @Test
    void adeptCostsTwoPicksOnTheSameProficiency() {
        npcs.grantPicks(npc, PICKS, "martial", "proficient", 3, "adept", 1);

        npcs.spendPick(npc, PICKS, "swords");
        assertEquals(Optional.of("proficient"), npcs.proficiencyTier(npc, "swords"));
        assertTrue(npcs.promote(npc, PICKS, "swords"));
        assertEquals(Optional.of("adept"), npcs.proficiencyTier(npc, "swords"));
        assertEquals(1, npcs.picks(npc, PICKS).get(0).unspent());
        assertFalse(npcs.promote(npc, PICKS, "swords"), "adept is the cap");
        assertFalse(npcs.promote(npc, PICKS, "shields"), "it was never picked");
    }

    @Test
    void picksWithoutAPromotionNeverPromote() {
        npcs.grantPicks(npc, PICKS, "martial", "proficient", 3, null, 0);
        npcs.spendPick(npc, PICKS, "swords");

        assertFalse(npcs.promote(npc, PICKS, "swords"));
    }

    @Test
    void aRespecGivesPicksAndPromotionsBack() {
        npcs.grantPicks(npc, PICKS, "martial", "proficient", 3, "adept", 1);
        npcs.spendPick(npc, PICKS, "swords");
        npcs.promote(npc, PICKS, "swords");

        assertEquals(2, npcs.respec(npc, PICKS));
        assertEquals(Optional.of("untrained"), npcs.proficiencyTier(npc, "swords"));
        assertEquals(3, npcs.takeUnspentPicks(npc, PICKS, 10));
        assertEquals(List.of(), npcs.picks(npc, PICKS));
    }

    @Test
    void anAbilityStaysWhileAnySourceHoldsIt() {
        assertTrue(npcs.grantAbility(npc, "keen-eye", SKILL));
        assertFalse(npcs.grantAbility(npc, "keen-eye", SKILL), "held already");
        assertTrue(npcs.grantAbility(npc, "keen-eye", MODULE));

        assertTrue(npcs.revokeAbility(npc, "keen-eye", SKILL));
        assertTrue(npcs.hasAbility(npc, "keen-eye"));
        assertFalse(npcs.holdsAbility(npc, "keen-eye", SKILL));
        assertTrue(npcs.revokeAbility(npc, "keen-eye", MODULE));
        assertFalse(npcs.hasAbility(npc, "keen-eye"));
        assertThrows(IllegalArgumentException.class, () -> npcs.grantAbility(npc, "flight", SKILL));
    }

    @Test
    void chosenAbilitiesSurviveTakingBackTheUnmadeChoicesButNotARespec() {
        npcs.grantAbilityChoice(npc, MODULE, List.of("keen-eye", "pack-mule", "haggler"), 2);
        npcs.chooseAbility(npc, MODULE, "haggler");

        assertEquals(1, npcs.takeUnspentAbilityChoices(npc, MODULE));
        assertTrue(npcs.hasAbility(npc, "haggler"));
        assertEquals(List.of(new AbilityChoiceGiven(List.of("keen-eye", "pack-mule", "haggler"), 1, List.of("haggler"))),
                npcs.abilityChoices(npc, MODULE));

        assertEquals(1, npcs.respec(npc, MODULE));
        assertFalse(npcs.hasAbility(npc, "haggler"));
        assertEquals(1, npcs.abilityChoices(npc, MODULE).get(0).remaining());
    }

    @Test
    void bakedStatsAreModifiersRemovedById() {
        String id = npcs.addModifier(npc, "strength", 2).orElseThrow();

        assertEquals(Map.of(id, new FakeSkysongNpcs.Modifier("strength", 2)), npcs.modifiers(npc));
        assertTrue(npcs.hasModifier(npc, id));
        assertEquals(Optional.empty(), npcs.addModifier(npc, "luck", 1), "no such stat");
        assertTrue(npcs.removeModifier(npc, id));
        assertFalse(npcs.removeModifier(npc, id));
    }

    @Test
    void fixedProficienciesAreSetOnTheSheet() {
        assertEquals(Optional.of("untrained"), npcs.proficiencyTier(npc, "perception"));
        assertTrue(npcs.setProficiencyTier(npc, "perception", "adept"));
        assertEquals(Optional.of("adept"), npcs.proficiencyTier(npc, "perception"));
        assertFalse(npcs.setProficiencyTier(npc, "perception", "legendary"));
        assertFalse(npcs.setProficiencyTier(npc, "juggling", "adept"));
    }

    @Test
    void aPolicyGivesGroupsActionsOnTheNpcsThatFollowIt() {
        assertTrue(npcs.createPolicy("economy.smithy"));
        assertFalse(npcs.createPolicy("economy.smithy"), "it exists already");
        npcs.setGrant("economy.smithy", "economy:smithy/workers", Set.of("info", "teleport"));
        npcs.assign(npc, "economy.smithy");

        assertEquals(Map.of("economy:smithy/workers", Set.of("info", "teleport")), npcs.grants("economy.smithy"));
        assertEquals(Optional.of("economy.smithy"), npcs.policyOf(npc));

        npcs.setGrant("economy.smithy", "economy:smithy/workers", Set.of());
        assertEquals(Map.of(), npcs.grants("economy.smithy"), "no actions, no grant");
        assertThrows(IllegalArgumentException.class,
                () -> npcs.setGrant("economy.smithy", "economy:smithy/workers", Set.of("fly")));
        assertThrows(IllegalArgumentException.class, () -> npcs.setGrant("economy.bakery", "perm:x", Set.of("info")));
    }

    @Test
    void deletingAPolicyLeavesItsNpcsWithout() {
        npcs.createPolicy("economy.smithy");
        npcs.assign(npc, "economy.smithy");

        assertTrue(npcs.deletePolicy("economy.smithy"));
        assertEquals(Optional.empty(), npcs.policyOf(npc));
        assertFalse(npcs.unassign(npc));
        assertFalse(npcs.hasPolicy("economy.smithy"));
    }
}
