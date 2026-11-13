package org.skysongdev.skysongEconomy.business.npc;

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
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessGroupDirectoryTest {
    private final CharacterRef owner = character("Owner");
    private final CharacterRef worker = character("Worker");
    private final CharacterRef customer = character("Customer");
    private final CharacterRef stranger = character("Stranger");
    private BusinessService businesses;
    private AccessService access;
    private BusinessGroupDirectory directory;
    private Business smithy;

    @BeforeEach
    void setUp() {
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("service", "Service", Set.of("martial"), true))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), Clock.systemUTC());
        access = new AccessService(new AccessDefaults(
                new AccessGroup(AccessGroup.WORKERS, Set.of(), BigDecimal.ZERO, Set.of()),
                new AccessGroup(AccessGroup.EVERYONE, Set.of(), BigDecimal.ZERO, Set.of()), 5),
                new InMemoryAccessRepository());
        directory = new BusinessGroupDirectory(businesses, access);

        smithy = businesses.create("smithy", "service", owner, new BlockPosition("world", 1, 64, 1));
        businesses.hire(smithy, worker);
        access.createGroup(smithy, "customers");
        access.addMember(smithy, "customers", customer);
    }

    private static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    private boolean contains(String group, CharacterRef character) {
        return directory.contains(group, character.player(), character.profile());
    }

    @Test
    void workersAreInTheWorkersGroup() {
        assertTrue(contains("smithy/workers", worker));
        assertFalse(contains("smithy/workers", customer));
        assertFalse(contains("smithy/workers", stranger));
    }

    @Test
    void everyCharacterIsInEveryone() {
        assertTrue(contains("smithy/everyone", stranger));
        assertTrue(contains("smithy/everyone", worker));
    }

    @Test
    void chosenMembersAreInTheOwnersGroups() {
        assertTrue(contains("smithy/customers", customer));
        assertFalse(contains("smithy/customers", worker));
    }

    @Test
    void membershipIsReadLive() {
        businesses.fire(smithy, worker);
        access.removeMember(smithy, "customers", customer);
        businesses.hire(smithy, stranger);

        assertFalse(contains("smithy/workers", worker));
        assertFalse(contains("smithy/customers", customer));
        assertTrue(contains("smithy/workers", stranger));
    }

    @Test
    void membershipBelongsToTheCharacterNotTheAccount() {
        assertFalse(directory.contains("smithy/workers", worker.player(), "Another Character"));
        assertFalse(directory.contains("smithy/workers", worker.player(), null), "no character, no group");
        assertFalse(directory.contains("smithy/everyone", stranger.player(), null));
    }

    @Test
    void unknownBusinessesGroupsAndBadNamesLetNobodyIn() {
        assertFalse(contains("bakery/workers", worker));
        assertFalse(contains("smithy/friends", worker));
        assertFalse(contains("smithy", worker));
        assertFalse(contains("smithy/workers/extra", worker));
    }

    @Test
    void theBusinessIdMayBeWrittenInAnyCase() {
        assertTrue(contains("SMITHY/workers", worker));
    }

    @Test
    void suggestsTheGroupsOfTheBusinessesTheCharacterOwns() {
        businesses.create("armory", "service", owner, new BlockPosition("world", 2, 64, 2));
        businesses.create("bakery", "service", stranger, new BlockPosition("world", 3, 64, 3));

        assertEquals(List.of("armory/workers", "armory/everyone", "smithy/workers", "smithy/everyone", "smithy/customers"),
                directory.suggestions(owner));
        assertEquals(List.of(), directory.suggestions(worker), "workers own nothing");
    }
}
