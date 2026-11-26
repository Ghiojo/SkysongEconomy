package org.skysongdev.skysongEconomy.business.bench;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.benches.BenchException;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BenchRegistryTest {
    private static final BlockPosition ANVIL = new BlockPosition("world", 10, 64, 10);
    private static final BlockPosition FORGE = new BlockPosition("world", 12, 64, 10);
    private static final BlockPosition DIRT = new BlockPosition("world", 14, 64, 10);

    private final List<String> announced = new ArrayList<>();
    private final TickingClock clock = new TickingClock();
    private InMemoryBenchRepository repository;
    private FakeBenchLookup lookup;
    private BusinessService businesses;
    private BenchRegistry benches;
    private Business smithy;
    private Business tannery;

    /** A clock that moves on a second every time it is read, so every link has its own time. */
    private static final class TickingClock extends Clock {
        private Instant now = Instant.parse("2026-11-26T12:00:00Z");

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
            now = now.plusSeconds(1);
            return now;
        }
    }

    @BeforeEach
    void setUp() {
        repository = new InMemoryBenchRepository();
        lookup = new FakeBenchLookup();
        lookup.benches.put(ANVIL, "anvil");
        lookup.benches.put(FORGE, "forge");
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("crafting", "Crafting", Set.of("metalworking"), false))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), clock);
        benches = new BenchRegistry(businesses, repository, lookup, clock);
        businesses.addListener(benches);
        benches.addListener(new BenchEvents() {
            @Override
            public void assigned(BusinessBench link, Optional<BusinessBench> previous) {
                announced.add("assigned " + link.businessId() + previous.map(old -> " from " + old.businessId()).orElse(""));
            }

            @Override
            public void unassigned(BusinessBench link, UnassignCause cause, Optional<String> by) {
                announced.add("unassigned " + link.businessId() + " " + cause + by.map(name -> " by " + name).orElse(""));
            }
        });
        CharacterRef owner = new CharacterRef(UUID.nameUUIDFromBytes("Owner".getBytes()), "Owner");
        smithy = businesses.create("smithy", "crafting", owner, new BlockPosition("world", 0, 64, 0));
        tannery = businesses.create("tannery", "crafting", owner, new BlockPosition("world", 0, 64, 5));
    }

    @Test
    void aBusinessCanHaveSeveralBenches() {
        benches.assign(ANVIL, "smithy", "Staff");
        benches.assign(FORGE, "SMITHY", "Staff");

        assertEquals(List.of(ANVIL, FORGE), benches.of("smithy").stream().map(BusinessBench::bench).toList());
        assertEquals("smithy", benches.at(FORGE).orElseThrow().businessId());
        assertEquals(2, repository.saved.size());
        assertEquals(List.of("assigned smithy", "assigned smithy"), announced);
    }

    @Test
    void assigningABenchOfAnotherBusinessMovesIt() {
        benches.assign(ANVIL, "smithy", "Staff");

        BenchRegistry.Assignment moved = benches.assign(ANVIL, "tannery", "Helper");

        assertEquals("smithy", moved.previous().orElseThrow().businessId());
        assertEquals("tannery", benches.at(ANVIL).orElseThrow().businessId());
        assertEquals("Helper", repository.saved.get(ANVIL).assignedBy());
        assertTrue(benches.of("smithy").isEmpty());
        assertEquals(List.of("assigned smithy", "assigned tannery from smithy"), announced);
    }

    @Test
    void assigningToTheSameBusinessIsRefused() {
        benches.assign(ANVIL, "smithy", "Staff");

        assertEquals("business.bench.already-assigned", refusal(() -> benches.assign(ANVIL, "smithy", "Staff")));
    }

    @Test
    void onlyBenchesOfKnownBusinessesCanBeAssigned() {
        assertEquals("business.bench.not-a-bench", refusal(() -> benches.assign(DIRT, "smithy", "Staff")));
        assertEquals("business.not-found", refusal(() -> benches.assign(ANVIL, "bakery", "Staff")));
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void nothingCanBeAssignedWithoutSkysongCrafting() {
        lookup.installed = false;

        assertEquals("business.bench.no-crafting", refusal(() -> benches.assign(ANVIL, "smithy", "Staff")));
    }

    @Test
    void unassigningForgetsTheLink() {
        benches.assign(ANVIL, "smithy", "Staff");

        assertEquals("smithy", benches.unassign(ANVIL, "Staff").orElseThrow().businessId());
        assertTrue(benches.unassign(ANVIL, "Staff").isEmpty());
        assertTrue(repository.saved.isEmpty());
        assertEquals("unassigned smithy UNASSIGNED by Staff", announced.get(1));
    }

    @Test
    void benchesGoWithADeletedBusiness() {
        benches.assign(ANVIL, "smithy", "Staff");
        benches.assign(FORGE, "tannery", "Staff");

        businesses.delete(smithy);

        assertTrue(benches.at(ANVIL).isEmpty());
        assertTrue(benches.at(FORGE).isPresent());
        assertTrue(announced.contains("unassigned smithy BUSINESS_DELETED"));
    }

    @Test
    void aBenchRemovedInSkysongCraftingIsUntied() {
        benches.assign(ANVIL, "smithy", "Staff");

        benches.benchRemoved(ANVIL);
        benches.benchRemoved(FORGE);

        assertTrue(benches.at(ANVIL).isEmpty());
        assertTrue(repository.saved.isEmpty());
        assertEquals(List.of("assigned smithy", "unassigned smithy BENCH_REMOVED"), announced);
    }

    @Test
    void benchesGoneOnStartupAreUntied() {
        benches.assign(ANVIL, "smithy", "Staff");
        benches.assign(FORGE, "tannery", "Staff");
        lookup.benches.remove(FORGE);
        List<BusinessBench> removed = new ArrayList<>();

        assertEquals(1, benches.removeMissing(removed::add));

        assertEquals(List.of(FORGE), removed.stream().map(BusinessBench::bench).toList());
        assertTrue(benches.at(ANVIL).isPresent());
        assertTrue(announced.contains("unassigned tannery BENCH_MISSING"));
    }

    @Test
    void linksStayWhileSkysongCraftingIsMissing() {
        benches.assign(ANVIL, "smithy", "Staff");
        lookup.installed = false;

        assertEquals(0, benches.removeMissing(link -> fail("nothing should be removed")));
        assertTrue(benches.at(ANVIL).isPresent());
    }

    @Test
    void loadsSavedLinks() {
        benches.assign(ANVIL, "smithy", "Staff");
        BenchRegistry reloaded = new BenchRegistry(businesses, repository, lookup, clock);

        assertEquals(1, reloaded.loadAll());
        assertEquals(benches.at(ANVIL), reloaded.at(ANVIL));
    }

    @Test
    void allIsSortedByBusinessThenAge() {
        benches.assign(FORGE, "tannery", "Staff");
        benches.assign(ANVIL, "smithy", "Staff");

        assertEquals(List.of("smithy", "tannery"), benches.all().stream().map(BusinessBench::businessId).toList());
    }

    private static String refusal(Runnable action) {
        return assertThrows(BenchException.class, action::run).messageKey();
    }
}
