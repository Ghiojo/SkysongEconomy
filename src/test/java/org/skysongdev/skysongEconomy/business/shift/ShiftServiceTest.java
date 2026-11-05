package org.skysongdev.skysongEconomy.business.shift;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessRepository;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;
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
import org.skysongdev.skysongEconomy.money.MapWallet;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.tokens.TokenRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftServiceTest {
    private static final BusinessTypeRegistry TYPES = new BusinessTypeRegistry(List.of(
            new BusinessType("gathering", "Gathering", Set.of("mining"), false)));
    /** Every tier allows 5 workers and adds 2 to the vault per worker shift. */
    private static final BusinessTiers TIERS = new BusinessTiers(IntStream.rangeClosed(1, 5)
            .mapToObj(tier -> new TierSettings(tier, 5, BigDecimal.valueOf(2))).toList());
    private static final ShiftSettings SETTINGS = new ShiftSettings(Duration.ofMinutes(30), 1,
            "{character} ({player}) started a shift at {business}",
            Map.of("gathering", Map.of(1, new ShiftPay(BigDecimal.valueOf(5), 1))));

    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Owner");
    private final CharacterRef worker = new CharacterRef(UUID.randomUUID(), "Worker");
    private final CharacterRef stranger = new CharacterRef(UUID.randomUUID(), "Stranger");

    private final Map<UUID, Integer> downtime = new HashMap<>();
    private final List<String> startMessages = new ArrayList<>();
    private final List<Shift> ended = new ArrayList<>();
    private final Map<UUID, Shift> savedShifts = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private boolean cancelStarts;

    private BusinessService businesses;
    private ActivityLock activities;
    private BusinessVaults vaults;
    private GatheringTokens tokens;
    private MapWallet wallet;
    private ShiftService shifts;
    private Business business;

    @BeforeEach
    void setUp() {
        businesses = new BusinessService(TYPES, TIERS, new NoBusinessStorage(),
                Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC));
        activities = new ActivityLock();
        wallet = new MapWallet();
        vaults = new BusinessVaults(new NoVaultStorage(), wallet, warnings::add);
        tokens = new GatheringTokens(new NoTokenStorage());
        shifts = newService();
        businesses.addListener(shifts);
        business = businesses.create("mine", "gathering", owner, new BlockPosition("world", 0, 64, 0));
        businesses.hire(business, worker);
        downtime.put(owner.player(), 5);
        downtime.put(worker.player(), 5);
        downtime.put(stranger.player(), 5);
    }

    private ShiftService newService() {
        DowntimeWallet downtimeWallet = new DowntimeWallet() {
            @Override
            public boolean trySpend(UUID player, int amount) {
                int left = available(player);
                if (left < amount) {
                    return false;
                }
                downtime.put(player, left - amount);
                return true;
            }

            @Override
            public int available(UUID player) {
                return downtime.getOrDefault(player, 0);
            }
        };
        ShiftEvents events = new ShiftEvents() {
            @Override
            public boolean starting(Business at, CharacterRef character, String message) {
                startMessages.add(message);
                return !cancelStarts;
            }

            @Override
            public void ended(Business at, Shift shift) {
                ended.add(shift);
            }
        };
        return new ShiftService(SETTINGS, new ShiftService.Dependencies(businesses, TIERS, activities, vaults, tokens,
                wallet, downtimeWallet, events), new MemoryShiftStorage(), Clock.systemUTC(), warnings::add);
    }

    private static String refusal(Runnable action) {
        return assertThrows(BusinessException.class, action::run).messageKey();
    }

    private Shift finishShift(CharacterRef character) {
        shifts.start(business, character, "player");
        return shifts.work(character.player(), Duration.ofMinutes(30)).orElseThrow();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void startingSpendsDowntimeLocksThePlayerAndFixesThePay() {
        Shift shift = shifts.start(business, worker, "Steve");

        assertEquals(4, downtime.get(worker.player()));
        assertEquals(Activity.Kind.SHIFT, activities.current(worker.player()).orElseThrow().kind());
        assertMoney("5", shift.pay().money());
        assertEquals(1, shift.pay().tokens());
        assertEquals(List.of("Worker (Steve) started a shift at mine"), startMessages);
    }

    @Test
    void theOwnersMessageReplacesTheDefault() {
        businesses.setShiftMessage(business, "{character} opened {business}");

        shifts.start(business, worker, "Steve");

        assertEquals(List.of("Worker opened mine"), startMessages);
    }

    @Test
    void onlyTheOwnerAndWorkersWorkShifts() {
        assertEquals("business.shift.not-worker", refusal(() -> shifts.start(business, stranger, "x")));
        shifts.start(business, owner, "x");
    }

    @Test
    void aPlayerWorksOneShiftAtATime() {
        shifts.start(business, worker, "x");

        assertEquals("business.activity.busy-shift", refusal(() -> shifts.start(business, worker, "x")));
    }

    @Test
    void refusesWithoutEnoughDowntimeAndSpendsNothing() {
        downtime.put(worker.player(), 0);

        assertEquals("business.shift.no-downtime", refusal(() -> shifts.start(business, worker, "x")));
        assertTrue(startMessages.isEmpty(), "no event for a shift that can't start");
    }

    @Test
    void aCancelledStartSpendsNothing() {
        cancelStarts = true;

        assertEquals("business.shift.cancelled", refusal(() -> shifts.start(business, worker, "x")));
        assertEquals(5, downtime.get(worker.player()));
        assertTrue(activities.current(worker.player()).isEmpty());
    }

    @Test
    void theShiftEndsOnceEnoughTimeIsWorked() {
        shifts.start(business, worker, "x");

        assertTrue(shifts.work(worker.player(), Duration.ofMinutes(29)).isEmpty());
        assertEquals(Duration.ofMinutes(1), shifts.active(worker.player()).orElseThrow().remaining());

        Shift done = shifts.work(worker.player(), Duration.ofMinutes(1)).orElseThrow();
        assertTrue(done.isFinished());
        assertEquals(List.of(done), ended);
        assertTrue(activities.current(worker.player()).isEmpty(), "the player is free again");
        assertTrue(shifts.active(worker.player()).isEmpty());
    }

    @Test
    void workingWithoutAShiftDoesNothing() {
        assertTrue(shifts.work(worker.player(), Duration.ofHours(1)).isEmpty());
    }

    @Test
    void aWorkersFinishedShiftAddsTheVaultCutButTheOwnersDoesnt() {
        finishShift(worker);
        assertMoney("2", vaults.balance(business));

        finishShift(owner);
        assertMoney("2", vaults.balance(business));
    }

    @Test
    void payCanOnlyBeRetrievedOnceTheShiftEnds() {
        shifts.start(business, worker, "x");
        assertTrue(shifts.owed(business, worker).isNothing());
        assertEquals("business.shift.nothing-owed", refusal(() -> shifts.retrieve(business, worker)));

        shifts.work(worker.player(), Duration.ofMinutes(30));
        assertMoney("5", shifts.owed(business, worker).money());
    }

    @Test
    void retrievingPaysMoneyToThePlayerAndTokensToTheCharacter() {
        finishShift(worker);
        finishShift(worker);

        ShiftPay paid = shifts.retrieve(business, worker);

        assertMoney("10", paid.money());
        assertMoney("10", wallet.balance(worker.player()));
        assertEquals(2, tokens.balance(worker));
        assertTrue(shifts.owed(business, worker).isNothing(), "paid shifts are gone");
        assertTrue(savedShifts.isEmpty(), "and deleted from storage");
    }

    @Test
    void aFailedPaymentPaysNothingAndKeepsThePayOwed() {
        finishShift(worker);
        wallet.refuseDeposits = true;

        assertEquals("business.shift.pay-failed", refusal(() -> shifts.retrieve(business, worker)));
        assertEquals(0, tokens.balance(worker));
        assertMoney("5", shifts.owed(business, worker).money());
    }

    @Test
    void firedWorkersKeepTheirPay() {
        finishShift(worker);
        businesses.fire(business, worker);

        assertMoney("5", shifts.retrieve(business, worker).money());
    }

    @Test
    void deletingTheBusinessPaysEveryShiftAndFreesThePlayers() {
        finishShift(owner);
        shifts.start(business, worker, "x");

        businesses.delete(business);

        assertMoney("5", wallet.balance(owner.player()));
        assertMoney("5", wallet.balance(worker.player()));
        assertTrue(activities.current(worker.player()).isEmpty());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void shiftsSurviveARestart() {
        finishShift(owner);
        shifts.start(business, worker, "x");
        shifts.work(worker.player(), Duration.ofMinutes(10));

        activities = new ActivityLock();
        ShiftService reloaded = newService();
        reloaded.loadAll();

        assertMoney("5", reloaded.owed(business, owner).money());
        assertEquals(Duration.ofMinutes(20), reloaded.active(worker.player()).orElseThrow().remaining());
        assertTrue(activities.current(worker.player()).isPresent(), "running shifts lock their players again");
    }

    @Test
    void aBusinessWithoutPayForItsTierStillRunsShifts() {
        businesses.setTier(business, 2);

        Shift shift = shifts.start(business, worker, "x");

        assertTrue(shift.pay().isNothing());
    }

    @Test
    void theRemainingTimeNeverGoesNegative() {
        Shift shift = Shift.start("mine", worker, Instant.EPOCH, Duration.ofMinutes(1), ShiftPay.NONE, BigDecimal.ZERO)
                .work(Duration.ofMinutes(5), Instant.EPOCH);

        assertEquals(Duration.ZERO, shift.remaining());
        assertFalse(shift.finished().isEmpty());
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

    private static final class NoVaultStorage implements VaultRepository {
        @Override
        public Map<String, BigDecimal> loadAll() {
            return Map.of();
        }

        @Override
        public void save(String businessId, BigDecimal balance) {
        }
    }

    private static final class NoTokenStorage implements TokenRepository {
        @Override
        public Map<CharacterRef, Long> loadAll() {
            return Map.of();
        }

        @Override
        public void save(CharacterRef character, long balance) {
        }
    }

    private final class MemoryShiftStorage implements ShiftRepository {
        @Override
        public List<Shift> loadAll() {
            return new ArrayList<>(savedShifts.values());
        }

        @Override
        public void save(Shift shift) {
            savedShifts.put(shift.id(), shift);
        }

        @Override
        public void delete(UUID shiftId) {
            savedShifts.remove(shiftId);
        }
    }
}
