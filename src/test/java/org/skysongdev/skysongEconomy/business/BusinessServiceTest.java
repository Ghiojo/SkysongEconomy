package org.skysongdev.skysongEconomy.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessServiceTest {
    private static final BusinessTypeRegistry TYPES = new BusinessTypeRegistry(List.of(
            new BusinessType("service", "Service", Set.of("martial"), true)));
    /** Tier N allows N workers. */
    private static final BusinessTiers TIERS = new BusinessTiers(
            IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList());
    private static final BlockPosition BLOCK = new BlockPosition("world", 1, 64, 1);
    private static final BlockPosition OTHER_BLOCK = new BlockPosition("world", 2, 64, 2);

    private final CharacterRef owner = character("Owner");
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-05T12:00:00Z"));
    private InMemoryBusinessRepository repository;
    private BusinessService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBusinessRepository();
        service = new BusinessService(TYPES, TIERS, repository, clock);
    }

    private static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    private static String refusal(Runnable action) {
        return assertThrows(BusinessException.class, action::run).messageKey();
    }

    @Test
    void createsATierOneBusinessNamedAfterItsId() {
        Business business = service.create("Smithy", "service", owner, BLOCK);

        assertEquals("smithy", business.id(), "ids are lower case");
        assertEquals("smithy", business.displayName());
        assertEquals(1, business.tier());
        assertTrue(business.isOwner(owner));
        assertEquals(business, service.at(BLOCK).orElseThrow());
        assertEquals(business, service.find("SMITHY").orElseThrow());
        assertEquals(business.snapshot(), repository.saved.get("smithy"));
    }

    @Test
    void refusesBadIdsTakenIdsUnknownTypesAndUsedBlocks() {
        service.create("smithy", "service", owner, BLOCK);

        assertEquals("business.invalid-id", refusal(() -> service.create("bad id", "service", owner, OTHER_BLOCK)));
        assertEquals("business.id-taken", refusal(() -> service.create("smithy", "service", owner, OTHER_BLOCK)));
        assertEquals("business.unknown-type", refusal(() -> service.create("bakery", "crafting", owner, OTHER_BLOCK)));
        assertEquals("business.block-taken", refusal(() -> service.create("bakery", "service", owner, BLOCK)));
    }

    @Test
    void movingFreesTheOldBlock() {
        Business business = service.create("smithy", "service", owner, BLOCK);

        service.move(business, OTHER_BLOCK);

        assertTrue(service.at(BLOCK).isEmpty());
        assertEquals(business, service.at(OTHER_BLOCK).orElseThrow());
        assertEquals(OTHER_BLOCK, repository.saved.get("smithy").position());
    }

    @Test
    void cannotMoveOntoAnotherBusiness() {
        Business first = service.create("smithy", "service", owner, BLOCK);
        service.create("bakery", "service", owner, OTHER_BLOCK);

        assertEquals("business.block-taken", refusal(() -> service.move(first, OTHER_BLOCK)));
        assertEquals(BLOCK, first.position().orElseThrow());
    }

    @Test
    void anUnplacedBusinessKeepsItsDataAndCanBePlacedAgain() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        service.hire(business, character("Worker"));

        service.unplace(business);

        assertTrue(business.position().isEmpty());
        assertTrue(service.at(BLOCK).isEmpty());
        assertEquals(1, business.workers().size());
        service.move(business, OTHER_BLOCK);
        assertEquals(business, service.at(OTHER_BLOCK).orElseThrow());
    }

    @Test
    void renameTrimsAndLimitsTheName() {
        Business business = service.create("smithy", "service", owner, BLOCK);

        service.rename(business, "  The Iron Anvil  ");

        assertEquals("The Iron Anvil", business.displayName());
        assertEquals("smithy", business.id(), "the id never changes");
        assertEquals("business.invalid-name", refusal(() -> service.rename(business, "   ")));
        assertEquals("business.invalid-name", refusal(() -> service.rename(business, "x".repeat(49))));
    }

    @Test
    void shiftMessageCanBeSetAndReset() {
        Business business = service.create("smithy", "service", owner, BLOCK);

        service.setShiftMessage(business, " {character} lights the forge ");
        assertEquals("{character} lights the forge", business.shiftMessage().orElseThrow());

        service.setShiftMessage(business, null);
        assertTrue(business.shiftMessage().isEmpty());
        assertEquals("business.invalid-shift-message", refusal(() -> service.setShiftMessage(business, "")));
    }

    @Test
    void hiringIsLimitedByTier() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        service.hire(business, character("A"));

        assertEquals("business.hire.full", refusal(() -> service.hire(business, character("B"))));
        service.setTier(business, 2);
        service.hire(business, character("B"));
        assertEquals(2, business.workers().size());
    }

    @Test
    void ownersAndWorkersCantBeHiredTwice() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        service.setTier(business, 3);
        CharacterRef worker = character("A");
        service.hire(business, worker);

        assertEquals("business.hire.is-owner", refusal(() -> service.hire(business, owner)));
        assertEquals("business.hire.already-worker", refusal(() -> service.hire(business, worker)));
    }

    @Test
    void aCharacterCanWorkAtSeveralBusinesses() {
        Business smithy = service.create("smithy", "service", owner, BLOCK);
        Business bakery = service.create("bakery", "service", owner, OTHER_BLOCK);
        CharacterRef worker = character("A");

        service.hire(smithy, worker);
        service.hire(bakery, worker);

        assertTrue(smithy.isWorker(worker));
        assertTrue(bakery.isWorker(worker));
    }

    @Test
    void anotherProfileOfTheSamePlayerIsADifferentCharacter() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        service.setTier(business, 2);
        CharacterRef alt = new CharacterRef(owner.player(), "Alt");

        service.hire(business, alt);

        assertTrue(business.isWorker(alt));
        assertFalse(business.isWorker(owner));
    }

    @Test
    void downgradeRemovesTheNewestHiresFirst() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        service.setTier(business, 3);
        CharacterRef first = character("First");
        CharacterRef second = character("Second");
        CharacterRef third = character("Third");
        service.hire(business, first);
        clock.advance(Duration.ofMinutes(1));
        service.hire(business, second);
        clock.advance(Duration.ofMinutes(1));
        service.hire(business, third);

        List<Hire> removed = service.setTier(business, 1);

        assertEquals(List.of(third, second), removed.stream().map(Hire::character).toList());
        assertEquals(List.of(first), business.workers().stream().map(Hire::character).toList());
        assertEquals(1, repository.saved.get("smithy").workers().size());
    }

    @Test
    void tierMustBeOneToFive() {
        Business business = service.create("smithy", "service", owner, BLOCK);

        assertEquals("business.invalid-tier", refusal(() -> service.setTier(business, 0)));
        assertEquals("business.invalid-tier", refusal(() -> service.setTier(business, 6)));
    }

    @Test
    void firingNeedsAWorker() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        CharacterRef worker = character("A");
        service.hire(business, worker);

        service.fire(business, worker);

        assertFalse(business.isWorker(worker));
        assertEquals("business.fire.not-worker", refusal(() -> service.fire(business, worker)));
    }

    @Test
    void aNewOwnerWhoWorkedThereStopsBeingAWorker() {
        Business business = service.create("smithy", "service", owner, BLOCK);
        CharacterRef worker = character("A");
        service.hire(business, worker);

        service.setOwner(business, worker);

        assertTrue(business.isOwner(worker));
        assertFalse(business.isWorker(worker));
        assertFalse(business.isMember(owner));
    }

    @Test
    void deleteRemovesTheBusinessAndItsBlock() {
        Business business = service.create("smithy", "service", owner, BLOCK);

        service.delete(business);

        assertTrue(service.find("smithy").isEmpty());
        assertTrue(service.at(BLOCK).isEmpty());
        assertTrue(repository.saved.isEmpty());
        assertEquals("business.not-found", refusal(() -> service.require("smithy")));
    }

    @Test
    void loadingRestoresBusinessesAndUnplacesDuplicateBlocks() {
        repository.saved.put("a", new BusinessSnapshot("a", "A", "service", 2, owner, BLOCK, "hi",
                List.of(new Hire(character("W"), Instant.EPOCH))));
        repository.saved.put("b", new BusinessSnapshot("b", "B", "service", 1, owner, BLOCK, null, List.of()));

        assertEquals(2, service.loadAll());

        Business a = service.find("a").orElseThrow();
        assertEquals("A", a.displayName());
        assertEquals(2, a.tier());
        assertEquals("hi", a.shiftMessage().orElseThrow());
        assertEquals(1, a.workers().size());
        assertEquals(a, service.at(BLOCK).orElseThrow());
        assertTrue(service.find("b").orElseThrow().position().isEmpty(), "the second business on a block loses it");
        assertTrue(repository.saved.get("b").position() == null, "and that is saved");
    }

    @Test
    void listenersHearAboutEveryChangeWithThePreviousBlock() {
        List<String> heard = new java.util.ArrayList<>();
        service.addListener(new BusinessChangeListener() {
            @Override
            public void changed(Business business, BlockPosition previousPosition) {
                heard.add("changed " + business.id() + " from " + previousPosition);
            }

            @Override
            public void deleted(Business business) {
                heard.add("deleted " + business.id());
            }

            @Override
            public void loaded(Iterable<Business> businesses) {
                heard.add("loaded");
            }
        });

        Business business = service.create("smithy", "service", owner, BLOCK);
        service.rename(business, "Anvil");
        service.move(business, OTHER_BLOCK);
        service.unplace(business);
        service.delete(business);
        service.loadAll();

        assertEquals(List.of(
                "changed smithy from null",
                "changed smithy from " + BLOCK,
                "changed smithy from " + BLOCK,
                "changed smithy from " + OTHER_BLOCK,
                "deleted smithy",
                "loaded"), heard);
    }

    /** A clock tests can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
