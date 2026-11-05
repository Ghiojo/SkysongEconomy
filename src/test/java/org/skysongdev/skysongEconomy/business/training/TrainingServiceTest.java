package org.skysongdev.skysongEconomy.business.training;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessRepository;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessRepository;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.business.activity.Activity;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.business.vault.VaultRepository;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.character.ProficiencyTraining;
import org.skysongdev.skysongEconomy.character.SkillProgression;
import org.skysongdev.skysongEconomy.money.MapWallet;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingServiceTest {
    private static final BusinessTypeRegistry TYPES = new BusinessTypeRegistry(List.of(
            new BusinessType("service", "Service", Set.of("medical", "martial"), true),
            new BusinessType("crafting", "Crafting", Set.of("smithing"), false)));
    /** Tier N caps proficiency at "cap-N". */
    private static final BusinessTiers TIERS = new BusinessTiers(IntStream.rangeClosed(1, 5)
            .mapToObj(tier -> new TierSettings(tier, 5, BigDecimal.ZERO, "cap-" + tier)).toList());
    private static final TrainingSettings SETTINGS = new TrainingSettings(Duration.ofMinutes(20), 1, Map.of(
            1, new TrainingReward(100, 1), 2, new TrainingReward(180, 2)));
    /** Workers train for 3; everyone else for 10. */
    private static final AccessDefaults ACCESS = new AccessDefaults(
            new AccessGroup(AccessGroup.WORKERS, EnumSet.of(Permission.TRAIN), BigDecimal.valueOf(3), Set.of()),
            new AccessGroup(AccessGroup.EVERYONE, EnumSet.of(Permission.TRAIN), BigDecimal.TEN, Set.of()), 4);

    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Owner");
    private final CharacterRef worker = new CharacterRef(UUID.randomUUID(), "Worker");
    private final CharacterRef visitor = new CharacterRef(UUID.randomUUID(), "Visitor");
    private final Map<UUID, Integer> downtime = new HashMap<>();
    private final List<String> xpGiven = new ArrayList<>();
    private final List<String> proficiencyTrained = new ArrayList<>();
    private final Map<UUID, TrainingSession> saved = new LinkedHashMap<>();

    private BusinessService businesses;
    private AccessService access;
    private ActivityLock activities;
    private BusinessVaults vaults;
    private MapWallet wallet;
    private TrainingService training;
    private Business dojo;

    @BeforeEach
    void setUp() {
        businesses = new BusinessService(TYPES, TIERS, new NoBusinessStorage(),
                Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC));
        access = new AccessService(ACCESS, new NoAccessStorage());
        activities = new ActivityLock();
        wallet = new MapWallet();
        vaults = new BusinessVaults(new NoVaultStorage(), wallet, warning -> { });
        training = newService();
        businesses.addListener(training);
        dojo = businesses.create("dojo", "service", owner, new BlockPosition("world", 0, 64, 0));
        businesses.hire(dojo, worker);
        businesses.setTier(dojo, 3);
        businesses.pick(dojo, "medical");
        businesses.pick(dojo, "martial");
        businesses.pick(dojo, "medical");
        for (CharacterRef character : List.of(owner, worker, visitor)) {
            downtime.put(character.player(), 5);
            wallet.balances.put(character.player(), BigDecimal.valueOf(50));
        }
    }

    private TrainingService newService() {
        DowntimeWallet downtimeWallet = new DowntimeWallet() {
            @Override
            public boolean trySpend(UUID player, int amount) {
                if (available(player) < amount) {
                    return false;
                }
                downtime.put(player, available(player) - amount);
                return true;
            }

            @Override
            public int available(UUID player) {
                return downtime.getOrDefault(player, 0);
            }
        };
        SkillProgression progression = new SkillProgression() {
            @Override
            public boolean addXp(CharacterRef character, String skillId, int amount) {
                xpGiven.add(character.profile() + ":" + skillId + "+" + amount);
                return true;
            }

            @Override
            public ProficiencyTraining trainProficiency(CharacterRef character, String skillId, int points, String maxTierId) {
                proficiencyTrained.add(skillId + "+" + points + "<=" + maxTierId);
                return skillId.equals("medical") ? ProficiencyTraining.TRAINED : ProficiencyTraining.OTHER_SKILL;
            }
        };
        return new TrainingService(SETTINGS, new TrainingService.Dependencies(businesses, TYPES, TIERS, access, activities,
                vaults, wallet, downtimeWallet, progression), new MemoryTrainingStorage(), Clock.systemUTC(), warning -> { });
    }

    private static String refusal(Runnable action) {
        return assertThrows(BusinessException.class, action::run).messageKey();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void startingChargesTheGroupPriceIntoTheVaultAndSpendsDowntime() {
        training.start(dojo, visitor);

        assertMoney("40", wallet.balance(visitor.player()));
        assertMoney("10", vaults.balance(dojo));
        assertEquals(4, downtime.get(visitor.player()));
        assertEquals(Activity.Kind.TRAINING, activities.current(visitor.player()).orElseThrow().kind());
    }

    @Test
    void workersPayTheirLowerPriceAndTheOwnerTrainsFree() {
        training.start(dojo, worker);
        training.start(dojo, owner);

        assertMoney("47", wallet.balance(worker.player()));
        assertMoney("50", wallet.balance(owner.player()));
        assertMoney("3", vaults.balance(dojo));
    }

    @Test
    void refusesThoseNoGroupLetsTrain() {
        access.setPermission(dojo, AccessGroup.EVERYONE, Permission.TRAIN, false);

        assertEquals("business.training.not-allowed", refusal(() -> training.start(dojo, visitor)));
        training.start(dojo, worker);
    }

    @Test
    void refusesWithoutPicksMoneyOrDowntime() {
        Business empty = businesses.create("empty", "service", owner, new BlockPosition("world", 1, 64, 0));
        assertEquals("business.training.no-skills", refusal(() -> training.start(empty, visitor)));

        wallet.balances.put(visitor.player(), BigDecimal.ONE);
        assertEquals("business.training.cannot-afford", refusal(() -> training.start(dojo, visitor)));

        downtime.put(worker.player(), 0);
        assertEquals("business.training.no-downtime", refusal(() -> training.start(dojo, worker)));
        assertMoney("50", wallet.balance(worker.player()));
    }

    @Test
    void sharesTheLockWithShifts() {
        activities.claim(visitor.player(), new Activity(Activity.Kind.SHIFT, "mine", visitor));

        assertEquals("business.activity.busy-shift", refusal(() -> training.start(dojo, visitor)));
        assertMoney("50", wallet.balance(visitor.player()));
    }

    @Test
    void aFinishedSessionGivesEachPickedSkillTheRewardForItsPickCount() {
        training.start(dojo, visitor);
        assertTrue(training.work(visitor.player(), Duration.ofMinutes(19)).isEmpty());

        TrainingResult result = training.work(visitor.player(), Duration.ofMinutes(1)).orElseThrow();

        assertEquals(List.of("Visitor:medical+180", "Visitor:martial+100"), xpGiven, "medical was picked twice");
        assertEquals(List.of("medical+2<=cap-3", "martial+1<=cap-3"), proficiencyTrained,
                "service trains proficiency up to the tier's cap");
        assertEquals(ProficiencyTraining.TRAINED, result.skills().get(0).proficiency());
        assertTrue(activities.current(visitor.player()).isEmpty());
        assertTrue(saved.isEmpty(), "finished sessions are deleted");
    }

    @Test
    void onlyServiceBusinessesTrainProficiency() {
        Business smithy = businesses.create("smithy", "crafting", owner, new BlockPosition("world", 2, 64, 0));
        businesses.pick(smithy, "smithing");
        training.start(smithy, visitor);

        TrainingResult result = training.work(visitor.player(), Duration.ofMinutes(20)).orElseThrow();

        assertEquals(List.of("Visitor:smithing+100"), xpGiven);
        assertTrue(proficiencyTrained.isEmpty());
        assertNull(result.skills().get(0).proficiency());
    }

    @Test
    void sessionsSurviveARestart() {
        training.start(dojo, visitor);
        training.work(visitor.player(), Duration.ofMinutes(5));

        activities = new ActivityLock();
        TrainingService reloaded = newService();
        reloaded.loadAll();

        assertEquals(Duration.ofMinutes(15), reloaded.active(visitor.player()).orElseThrow().remaining());
        assertTrue(activities.current(visitor.player()).isPresent());
    }

    @Test
    void deletingTheBusinessFinishesItsSessionsAtOnce() {
        training.start(dojo, visitor);

        businesses.delete(dojo);

        assertEquals(2, xpGiven.size());
        assertTrue(activities.current(visitor.player()).isEmpty());
        assertEquals(Optional.empty(), training.active(visitor.player()));
    }

    private static final class NoBusinessStorage implements BusinessRepository {
        @Override
        public List<BusinessSnapshot> loadAll() {
            return List.of();
        }

        @Override
        public void save(BusinessSnapshot business) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class NoAccessStorage implements AccessRepository {
        @Override
        public Map<String, List<AccessGroup>> loadAll() {
            return Map.of();
        }

        @Override
        public void save(String businessId, List<AccessGroup> groups) {
        }
    }

    private static final class NoVaultStorage implements VaultRepository {
        @Override
        public Map<String, BigDecimal> loadAll() {
            return Map.of();
        }

        @Override
        public void save(String businessId, BigDecimal balance) {
        }
    }

    private final class MemoryTrainingStorage implements TrainingRepository {
        @Override
        public List<TrainingSession> loadAll() {
            return new ArrayList<>(saved.values());
        }

        @Override
        public void save(TrainingSession session) {
            saved.put(session.id(), session);
        }

        @Override
        public void delete(UUID sessionId) {
            saved.remove(sessionId);
        }
    }
}
