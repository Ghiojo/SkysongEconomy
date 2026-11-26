package org.skysongdev.skysongEconomy.business.bench;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.benches.BenchLink;
import org.skysongdev.skysongEconomy.api.benches.BenchPosition;
import org.skysongdev.skysongEconomy.api.benches.BenchUnassignCause;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** The public service shows the registry's links in the API's types, with the bench's current type. */
class BenchServiceTest {
    private static final BenchPosition ANVIL = new BenchPosition("world", 10, 64, 10);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);

    private FakeBenchLookup lookup;
    private BenchService service;

    @BeforeEach
    void setUp() {
        lookup = new FakeBenchLookup();
        lookup.benches.put(BenchService.block(ANVIL), "anvil");
        BusinessService businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("crafting", "Crafting", Set.of("metalworking"), false))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), CLOCK);
        businesses.create("smithy", "crafting", new CharacterRef(UUID.randomUUID(), "Owner"),
                new BlockPosition("world", 0, 64, 0));
        service = new BenchService(new BenchRegistry(businesses, new InMemoryBenchRepository(), lookup, CLOCK));
    }

    @Test
    void assignedLinksCarryTheBenchType() {
        BenchLink link = service.assign(ANVIL, "smithy", "Staff");

        assertEquals(new BenchLink(ANVIL, "smithy", Optional.of("anvil"), "Staff", CLOCK.instant()), link);
        assertEquals(Optional.of(link), service.at(ANVIL));
        assertEquals(List.of(link), service.of("smithy"));
        assertEquals(List.of(link), service.all());
    }

    @Test
    void theTypeIsEmptyOnceTheBenchIsGone() {
        service.assign(ANVIL, "smithy", "Staff");
        lookup.benches.clear();

        assertEquals(Optional.empty(), service.at(ANVIL).orElseThrow().benchType());
    }

    @Test
    void unassignReturnsTheRemovedLink() {
        service.assign(ANVIL, "smithy", "Staff");

        assertEquals("smithy", service.unassign(ANVIL, "Staff").orElseThrow().businessId());
        assertTrue(service.at(ANVIL).isEmpty());
    }

    @Test
    void everyCauseHasAPublicOne() {
        for (UnassignCause cause : UnassignCause.values()) {
            assertEquals(cause.name(), BenchService.cause(cause).name());
        }
        assertEquals(BenchUnassignCause.values().length, UnassignCause.values().length);
    }

    @Test
    void positionsConvertBothWays() {
        assertEquals(ANVIL, BenchService.position(BenchService.block(ANVIL)));
    }
}
