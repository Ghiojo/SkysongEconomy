package org.skysongdev.skysongEconomy.upkeep;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpkeepServiceTest {
    private static final Instant START = Instant.parse("2026-11-17T12:00:00Z");
    private static final Duration DAY = Duration.ofDays(1);
    private static final UpkeepProfile RATIONS = new UpkeepProfile("rations", DAY, 9, List.of(
            new UpkeepCost(new UpkeepCost.Match.Item("water_flask"), 1),
            new UpkeepCost(new UpkeepCost.Match.GoodType("food"), 2)));
    private static final UpkeepProfile COINS = new UpkeepProfile("coins", Duration.ofDays(3), 9, List.of(
            new UpkeepCost(new UpkeepCost.Match.Item("coin"), 5)));
    private static final UpkeepSettings SETTINGS = new UpkeepSettings(Duration.ofSeconds(60),
            Map.of("rations", RATIONS, "coins", COINS), Map.of("guard", "rations"));

    private final UUID guard = UUID.randomUUID();
    private final UUID dog = UUID.randomUUID();
    private final FakeNpcs npcs = new FakeNpcs();
    private final FakeStock stock = new FakeStock();
    private final InMemoryUpkeepRepository repository = new InMemoryUpkeepRepository();
    private final List<String> events = new ArrayList<>();
    private final List<UUID> lost = new ArrayList<>();
    private final MutableClock clock = new MutableClock(START);
    private UpkeepService service;

    @BeforeEach
    void setUp() {
        npcs.add(guard, "guard");
        npcs.add(dog, "guard_dog");
        service = newService();
    }

    private UpkeepService newService() {
        UpkeepService created = new UpkeepService(SETTINGS, new UpkeepService.Dependencies(npcs, stock,
                new UpkeepPayment(id -> id.equals("bread") ? Set.of("food") : Set.of())), repository,
                (upkeep, cause) -> events.add(upkeep.state() + " " + cause), clock, warning -> { });
        created.onLost(lost::add);
        return created;
    }

    private NpcUpkeep upkeepOf(UUID npc) {
        return service.upkeep(npc).orElseThrow();
    }

    @Test
    void onlyAnNpcWithAnUpkeepCanBeUnmaintained() {
        service.syncTemplates();

        assertFalse(service.isUnmaintained(guard));
        assertFalse(service.isUnmaintained(dog));
        service.mark(guard, UpkeepState.UNMAINTAINED, UpkeepEvents.Cause.STAFF);
        assertTrue(service.isUnmaintained(guard));
        service.setByHand(guard, null);
        assertFalse(service.isUnmaintained(guard));
    }

    @Test
    void npcsFromAListedTemplateGetItsProfileAndStartMaintained() {
        assertEquals(1, service.syncTemplates());

        NpcUpkeep upkeep = upkeepOf(guard);
        assertEquals(Optional.of("rations"), upkeep.profileId());
        assertEquals(UpkeepState.MAINTAINED, upkeep.state());
        assertEquals(START.plus(DAY), upkeep.nextDue());
        assertFalse(upkeep.setByHand());
        assertTrue(service.upkeep(dog).isEmpty());
        assertEquals(upkeep, repository.rows.get(guard));
    }

    @Test
    void aDueCycleEatsItsCostWhenTheStockHoldsIt() {
        service.syncTemplates();
        stock.put(guard, new StockStack("water_flask", 2), new StockStack("bread", 5));
        clock.set(START.plus(DAY));

        assertEquals(0, service.settle());

        assertEquals(List.of(new StockStack("water_flask", 1), new StockStack("bread", 3)), stock.view(guard));
        assertEquals(UpkeepState.MAINTAINED, upkeepOf(guard).state());
        assertEquals(START.plus(DAY.multipliedBy(2)), upkeepOf(guard).nextDue());
        assertTrue(events.isEmpty());
    }

    @Test
    void cyclesThatAreNotDueYetEatNothing() {
        service.syncTemplates();
        stock.put(guard, new StockStack("water_flask", 2), new StockStack("bread", 5));
        clock.set(START.plus(DAY).minusSeconds(1));

        service.settle();

        assertEquals(List.of(new StockStack("water_flask", 2), new StockStack("bread", 5)), stock.view(guard));
    }

    @Test
    void aCycleThatCantBePaidTakesNothingAndMakesTheNpcUnmaintained() {
        service.syncTemplates();
        stock.put(guard, new StockStack("water_flask", 2), new StockStack("bread", 1));
        clock.set(START.plus(DAY));

        assertEquals(1, service.settle());

        assertEquals(List.of(new StockStack("water_flask", 2), new StockStack("bread", 1)), stock.view(guard));
        assertEquals(UpkeepState.UNMAINTAINED, upkeepOf(guard).state());
        assertEquals(START.plus(DAY), upkeepOf(guard).changed());
        assertEquals(List.of("UNMAINTAINED CYCLE"), events);
    }

    @Test
    void unmaintainedNpcsEatNothingEvenWhenRestocked() {
        service.syncTemplates();
        clock.set(START.plus(DAY));
        service.settle();
        stock.put(guard, new StockStack("water_flask", 9), new StockStack("bread", 9));
        clock.set(START.plus(DAY.multipliedBy(5)));

        assertEquals(0, service.settle());

        assertEquals(List.of(new StockStack("water_flask", 9), new StockStack("bread", 9)), stock.view(guard));
        assertEquals(UpkeepState.UNMAINTAINED, upkeepOf(guard).state());
    }

    @Test
    void cyclesMissedWhileTheServerWasDownAreSettledInOrder() {
        service.syncTemplates();
        stock.put(guard, new StockStack("water_flask", 3), new StockStack("bread", 4));
        clock.set(START.plus(DAY.multipliedBy(3)).plusSeconds(1));

        service.settle();

        // Two cycles were paid, the third found one bread and lapsed.
        assertEquals(List.of(new StockStack("water_flask", 1)), stock.view(guard));
        assertEquals(UpkeepState.UNMAINTAINED, upkeepOf(guard).state());
        assertEquals(START.plus(DAY.multipliedBy(3)), upkeepOf(guard).nextDue());
    }

    @Test
    void markingMaintainedRestartsTheTimerAndEatingStartsAgain() {
        service.syncTemplates();
        clock.set(START.plus(DAY));
        service.settle();
        clock.set(START.plus(DAY.multipliedBy(4)));

        NpcUpkeep repaired = service.mark(guard, UpkeepState.MAINTAINED, UpkeepEvents.Cause.REPAIR).orElseThrow();

        assertEquals(UpkeepState.MAINTAINED, repaired.state());
        assertEquals(START.plus(DAY.multipliedBy(5)), repaired.nextDue());
        assertEquals(List.of("UNMAINTAINED CYCLE", "MAINTAINED REPAIR"), events);
        stock.put(guard, new StockStack("water_flask", 1), new StockStack("bread", 2));
        clock.set(START.plus(DAY.multipliedBy(5)));
        service.settle();
        assertTrue(stock.view(guard).isEmpty());
        assertEquals(UpkeepState.MAINTAINED, upkeepOf(guard).state());
    }

    @Test
    void markingAnNpcWithoutUpkeepDoesNothing() {
        assertTrue(service.mark(dog, UpkeepState.UNMAINTAINED, UpkeepEvents.Cause.STAFF).isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void staffCanGiveAnyNpcAProfileByHand() {
        service.syncTemplates();

        NpcUpkeep set = service.setByHand(dog, "coins");

        assertEquals(Optional.of("coins"), set.profileId());
        assertTrue(set.setByHand());
        assertEquals(START.plus(Duration.ofDays(3)), set.nextDue());
        assertThrows(IllegalArgumentException.class, () -> service.setByHand(dog, "nothing"));
        assertThrows(IllegalArgumentException.class, () -> service.setByHand(UUID.randomUUID(), "coins"));
    }

    @Test
    void aProfileSetByHandWinsOverTheTemplateUntilStaffHandItBack() {
        service.syncTemplates();
        service.setByHand(guard, "coins");

        service.syncTemplates();
        assertEquals(Optional.of("coins"), upkeepOf(guard).profileId());

        assertEquals(Optional.of("rations"), service.clearHand(guard).flatMap(NpcUpkeep::profileId));
        assertFalse(upkeepOf(guard).setByHand());
    }

    @Test
    void takingTheUpkeepAwayByHandKeepsTheTemplateFromGivingItBack() {
        service.syncTemplates();

        service.setByHand(guard, null);
        service.syncTemplates();

        assertTrue(service.upkeep(guard).isEmpty());
        assertTrue(service.saved(guard).orElseThrow().setByHand());
        assertEquals(List.of(guard), lost);
        assertTrue(service.all().isEmpty());
    }

    @Test
    void changingProfileNeverRepairsAnUnmaintainedNpc() {
        service.syncTemplates();
        clock.set(START.plus(DAY));
        service.settle();

        service.setByHand(guard, "coins");

        assertEquals(UpkeepState.UNMAINTAINED, upkeepOf(guard).state());
    }

    @Test
    void anNpcWhoseTemplateIsNoLongerListedLosesTheUpkeepItGave() {
        service.syncTemplates();
        npcs.add(guard, "farmhand");

        service.syncTemplates();

        assertTrue(service.upkeep(guard).isEmpty());
        assertFalse(repository.rows.containsKey(guard));
        assertEquals(List.of(guard), lost);
    }

    @Test
    void npcsThatAreGoneAreForgotten() {
        service.syncTemplates();
        service.setByHand(dog, "coins");
        npcs.remove(dog);

        service.syncTemplates();
        assertTrue(service.saved(dog).isEmpty());

        service.npcRemoved(guard);
        assertTrue(service.saved(guard).isEmpty());
        assertEquals(List.of(dog, guard), lost);
    }

    @Test
    void upkeepsAreReadBackAfterARestart() {
        service.syncTemplates();
        clock.set(START.plus(DAY));
        service.settle();

        UpkeepService restarted = newService();
        assertEquals(1, restarted.loadAll());

        assertEquals(UpkeepState.UNMAINTAINED, restarted.upkeep(guard).orElseThrow().state());
    }

    @Test
    void listsUpkeepsSoonestFirstAndByState() {
        service.setByHand(dog, "coins");
        service.syncTemplates();
        UUID third = UUID.randomUUID();
        npcs.add(third, "guard");
        clock.set(START.plus(Duration.ofHours(1)));
        service.syncTemplates();

        assertEquals(List.of(guard, third, dog), service.all().stream().map(NpcUpkeep::npc).toList());
        service.mark(third, UpkeepState.UNMAINTAINED, UpkeepEvents.Cause.STAFF);
        assertEquals(List.of(third), service.all(UpkeepState.UNMAINTAINED).stream().map(NpcUpkeep::npc).toList());
    }

    /** SkysongNPCs' NPCs, by id, with their templates. */
    private static final class FakeNpcs implements UpkeepNpcs {
        private final Map<UUID, String> templates = new LinkedHashMap<>();

        void add(UUID npc, String template) {
            templates.put(npc, template);
        }

        void remove(UUID npc) {
            templates.remove(npc);
        }

        @Override
        public List<Known> all() {
            return templates.entrySet().stream().map(entry -> new Known(entry.getKey(), Optional.of(entry.getValue()))).toList();
        }

        @Override
        public boolean exists(UUID npc) {
            return templates.containsKey(npc);
        }

        @Override
        public Optional<String> name(UUID npc) {
            return Optional.ofNullable(templates.get(npc));
        }
    }

    /** Stocks as plain stacks. */
    private static final class FakeStock implements UpkeepStock {
        private final Map<UUID, List<StockStack>> stacks = new HashMap<>();

        void put(UUID npc, StockStack... held) {
            stacks.put(npc, List.of(held));
        }

        @Override
        public List<StockStack> view(UUID npc) {
            return stacks.getOrDefault(npc, List.of());
        }

        @Override
        public void take(UUID npc, List<Integer> taken) {
            List<StockStack> left = new ArrayList<>();
            List<StockStack> held = view(npc);
            for (int i = 0; i < held.size(); i++) {
                int amount = held.get(i).amount() - taken.get(i);
                if (amount > 0) {
                    left.add(new StockStack(held.get(i).materialId(), amount));
                }
            }
            stacks.put(npc, left);
        }
    }

    private static final class InMemoryUpkeepRepository implements UpkeepRepository {
        private final Map<UUID, NpcUpkeep> rows = new LinkedHashMap<>();

        @Override
        public List<NpcUpkeep> loadAll() {
            return List.copyOf(rows.values());
        }

        @Override
        public void save(NpcUpkeep upkeep) {
            rows.put(upkeep.npc(), upkeep);
        }

        @Override
        public void delete(UUID npc) {
            rows.remove(npc);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
