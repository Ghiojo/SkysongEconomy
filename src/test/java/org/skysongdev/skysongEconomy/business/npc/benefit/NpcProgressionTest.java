package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.InMemoryAccessRepository;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.FakeSkysongNpcs;
import org.skysongdev.skysongEconomy.business.npc.InMemoryBusinessNpcRepository;
import org.skysongdev.skysongEconomy.business.npc.NpcPending;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSettingsParser;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.SpawnPoint;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcProgressionTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical")::contains;
    private static final SlotSource MARTIAL = SlotSource.skill("martial");
    private static final String SOURCE = "economy:skill:martial";
    private static final String PICKS = SOURCE + ":picks";
    /**
     * Agustín's martial example: one NPC per martial pick, 3 + level stat points, 2 + level
     * picks that reach adept from level 4, plus a baked stat, an ability at 2 and a fixed
     * proficiency at 3.
     */
    private static final String CONFIG = """
            skills:
              martial:
                slots: "{level}"
                template: guard
                progression:
                  stat-points: {amount: "3 + {level}"}
                  stats: {strength: 1}
                  proficiency-picks:
                    skill: martial
                    amount: "2 + {level}"
                    max-tier: {by: level, 1: proficient, 4: adept}
                    promote-cost: 1
                  at:
                    by: level
                    2: {abilities: [keen-eye]}
                    3: {proficiencies: {shields: proficient}}
            """;

    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Aldric");
    private final List<String> warnings = new ArrayList<>();
    private final List<String> noticed = new ArrayList<>();
    private final FakeSkysongNpcs skysongNpcs = new FakeSkysongNpcs();
    private final InMemoryBusinessNpcRepository npcRepository = new InMemoryBusinessNpcRepository();
    private final InMemoryNpcAppliedRepository appliedRepository = new InMemoryNpcAppliedRepository();
    private BusinessService businesses;
    private AccessService access;
    private BusinessNpcs npcs;
    private NpcProgression progression;
    private Business business;

    @BeforeEach
    void setUp() {
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("service", "Service", Set.of("martial", "economical"), true))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), Clock.systemUTC());
        access = new AccessService(new AccessDefaults(
                new AccessGroup(AccessGroup.WORKERS, Set.of(), BigDecimal.ZERO, Set.of()),
                new AccessGroup(AccessGroup.EVERYONE, Set.of(), BigDecimal.ZERO, Set.of()), 5),
                new InMemoryAccessRepository());
        start(CONFIG);
        business = businesses.create("barracks", "service", owner, new BlockPosition("world", 1, 64, 1));
    }

    /** Builds the services from a config file, the way the plugin wires them on start. */
    private void start(String config) {
        BenefitParser benefits = new BenefitParser(SKILLS, skysongNpcs);
        NpcSettings settings = new NpcSettingsParser(SKILLS, benefits, warnings::add)
                .parse(YamlFiles.parse(NpcSettingsParser.FILE, new StringReader(config)));
        npcs = new BusinessNpcs(settings, new BusinessNpcs.Dependencies(businesses, access, skysongNpcs), npcRepository,
                BusinessNpcEvents.NONE, Clock.systemUTC(), warnings::add);
        progression = new NpcProgression(settings, new NpcProgression.Dependencies(businesses, npcs, skysongNpcs),
                benefits.tiers(), appliedRepository,
                (changed, npc, changes) -> noticed.add(changes.choicesReset() ? "reset" : changes.picksReset() ? "picks" : "more"),
                warnings::add);
        npcs.addListener(progression);
        businesses.addListener(npcs);
        businesses.addListener(progression);
    }

    /** Raises the business to that tier and spends every free pick on martial. */
    private void martial(int tier) {
        businesses.setTier(business, tier);
        while (business.freePicks() > 0) {
            businesses.pick(business, "martial");
        }
    }

    private UUID create() {
        UUID npc = npcs.create(business, MARTIAL, owner.player()).npc();
        skysongNpcs.completeSetup(npc);
        return npc;
    }

    @Test
    void aNewNpcReceivesWhatItsSlotOwesAtOnce() {
        martial(1);

        UUID npc = create();

        assertEquals(new NpcPending(4, 3, 0), skysongNpcs.pending(npc), "3 + 1 points and 2 + 1 picks");
        assertEquals(List.of(new FakeSkysongNpcs.Modifier("strength", 1)), List.copyOf(skysongNpcs.modifiers(npc).values()));
        assertFalse(skysongNpcs.hasAbility(npc, "keen-eye"));
        assertTrue(noticed.isEmpty(), "whoever creates it is in its setup already");
        assertEquals(1, appliedRepository.saved.size());
    }

    @Test
    void aSecondPickGivesEveryNpcTheDifferenceAndTellsTheOwner() {
        martial(1);
        UUID npc = create();
        skysongNpcs.spendPoints(npc, SOURCE, 4);

        martial(2);

        assertEquals(new NpcPending(1, 4, 0), skysongNpcs.pending(npc), "one more point and one more pick");
        assertTrue(skysongNpcs.hasAbility(npc, "keen-eye"), "the level 2 ability");
        assertTrue(noticed.contains("more"));
        assertEquals(1, npcs.slots(business).get(0).free(), "and a second slot opened");
    }

    @Test
    void aFixedProficiencyArrivesAtItsStepAndGoesBackBelowIt() {
        martial(3);
        UUID npc = create();
        assertEquals(Optional.of("proficient"), skysongNpcs.proficiencyTier(npc, "shields"));

        businesses.clearPick(business, 3);

        assertEquals(Optional.of("untrained"), skysongNpcs.proficiencyTier(npc, "shields"));
    }

    @Test
    void aClearedPickGivesEverythingBackAtTheLowerAmounts() {
        martial(2);
        UUID npc = create();
        skysongNpcs.spendPoints(npc, SOURCE, 5);
        skysongNpcs.spendPick(npc, PICKS, "swords");

        businesses.clearPick(business, 2);

        assertEquals(new NpcPending(4, 3, 0), skysongNpcs.pending(npc));
        assertEquals(Optional.of("untrained"), skysongNpcs.proficiencyTier(npc, "swords"));
        assertFalse(skysongNpcs.hasAbility(npc, "keen-eye"));
        assertEquals(1, skysongNpcs.modifiers(npc).size(), "the baked stat is owed at every level");
        assertTrue(noticed.contains("reset"));
    }

    @Test
    void reachingLevelFourOpensAdeptAndOnlyThePicksArePickedAgain() {
        martial(3);
        UUID npc = create();
        skysongNpcs.spendPoints(npc, SOURCE, 6);
        skysongNpcs.spendPick(npc, PICKS, "swords");
        noticed.clear();

        martial(4);

        assertEquals(new NpcPending(1, 6, 0), skysongNpcs.pending(npc), "6 spent points stay, all 6 picks are open");
        skysongNpcs.spendPick(npc, PICKS, "swords");
        assertTrue(skysongNpcs.promote(npc, PICKS, "swords"));
        assertEquals(Optional.of("adept"), skysongNpcs.proficiencyTier(npc, "swords"));
        assertEquals(List.of("picks"), noticed.stream().distinct().toList());
    }

    @Test
    void afterARestartNothingIsGivenTwice() {
        martial(2);
        UUID npc = create();
        NpcPending before = skysongNpcs.pending(npc);

        start(CONFIG);
        npcs.loadAll();
        int changed = progression.loadAll();

        assertEquals(0, changed);
        assertEquals(before, skysongNpcs.pending(npc));
        assertEquals(1, skysongNpcs.modifiers(npc).size());
    }

    @Test
    void configEditedWhileTheServerWasOffAppliesOnStart() {
        martial(1);
        UUID npc = create();

        start(CONFIG.replace("strength: 1", "strength: 2").replace("\"3 + {level}\"", "\"5 + {level}\""));
        npcs.loadAll();
        int changed = progression.loadAll();

        assertEquals(1, changed);
        assertEquals(6, skysongNpcs.pending(npc).points());
        assertEquals(List.of(new FakeSkysongNpcs.Modifier("strength", 2)), List.copyOf(skysongNpcs.modifiers(npc).values()));
    }

    @Test
    void anNpcWhoseSkillLeftTheFileIsLeftAsItIs() {
        martial(1);
        UUID npc = create();
        NpcPending before = skysongNpcs.pending(npc);

        start("skills: {}");
        npcs.loadAll();
        progression.loadAll();

        assertEquals(before, skysongNpcs.pending(npc));
        assertEquals(1, skysongNpcs.modifiers(npc).size());
        assertEquals(Optional.empty(), progression.owed(business, npcs.find(npc).orElseThrow()));
    }

    @Test
    void aDismissedNpcIsForgotten() {
        martial(1);
        UUID npc = create();

        npcs.dismiss(business, npc);

        assertTrue(appliedRepository.saved.isEmpty());
    }

    @Test
    void aRespecInSkysongNpcsLeavesTheNpcInLine() {
        martial(1);
        UUID npc = create();
        skysongNpcs.spendPoints(npc, SOURCE, 4);
        skysongNpcs.respec(npc, SOURCE);

        progression.respecced(npc);

        assertEquals(new NpcPending(4, 3, 0), skysongNpcs.pending(npc), "the same grants, unspent");
        progression.respecced(UUID.randomUUID());
    }

    @Test
    void aCopyOfABusinessNpcLosesWhatTheBusinessGave() {
        martial(2);
        UUID npc = create();
        UUID copy = skysongNpcs.create("guard", owner, new SpawnPoint.InFrontOf(owner.player()), 3).npc().orElseThrow();
        skysongNpcs.grantPoints(copy, SOURCE, 5, List.of());
        skysongNpcs.grantAbility(copy, "keen-eye", SOURCE);
        skysongNpcs.grantPoints(copy, "template", 2, List.of());

        progression.copied(npc, copy);

        assertEquals(2, skysongNpcs.pending(copy).points(), "only the template's points are left");
        assertFalse(skysongNpcs.hasAbility(copy, "keen-eye"));
        assertEquals(5, skysongNpcs.pending(npc).points(), "the original keeps everything");
    }

    @Test
    void anNpcSkysongNpcsRefusesIsReportedAndTheOthersStillChange() {
        martial(2);
        UUID first = create();
        UUID second = create();
        // Gone in SkysongNPCs without the business hearing of it.
        skysongNpcs.remove(first);

        martial(3);

        assertEquals(6, skysongNpcs.pending(second).points(), "3 + 3 for the NPC that is still there");
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains(first.toString())), warnings.toString());
    }

    @Test
    void staffCanAskWhatAnNpcIsOwed() {
        martial(4);
        BusinessNpc npc = npcs.find(create()).orElseThrow();

        Entitlement owed = progression.owed(business, npc).orElseThrow();

        assertEquals(7, owed.totalPoints());
        assertEquals(6, owed.totalPicks());
        assertEquals(List.of(new PickKind("martial", "proficient", "adept", 1)), owed.pickKinds());
        assertEquals(Set.of("keen-eye"), owed.abilities());
    }
}
