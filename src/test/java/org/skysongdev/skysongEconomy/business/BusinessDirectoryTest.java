package org.skysongdev.skysongEconomy.business;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.business.BusinessBlock;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessDirectoryTest {
    private final BusinessService service = new BusinessService(
            new BusinessTypeRegistry(List.of(new BusinessType("crafting", "Crafting", Set.of("cooking"), false))),
            new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
            new InMemoryBusinessRepository(), Clock.systemUTC());
    private final BusinessDirectory directory = new BusinessDirectory(service);
    private final CharacterRef owner = character("Owner");
    private final CharacterRef worker = character("Worker");

    private static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    @Test
    void copiesABusinessWithItsOwnerAndWorkers() {
        Business business = service.create("smithy", "crafting", owner, new BlockPosition("world", 1, 64, 1));
        service.rename(business, "Rose Smithy");
        service.hire(business, worker);

        BusinessInfo info = directory.find("SMITHY").orElseThrow();

        assertEquals(new BusinessInfo("smithy", "crafting", "Rose Smithy", 1, Optional.of(owner), List.of(worker),
                Optional.of(new BusinessBlock("world", 1, 64, 1))), info);
        assertFalse(info.serverOwned());
    }

    @Test
    void marksServerOwnedBusinesses() {
        service.createServerOwned("docks", "crafting", new BlockPosition("world", 2, 64, 2));

        BusinessInfo info = directory.find("docks").orElseThrow();

        assertTrue(info.serverOwned());
        assertEquals(Optional.empty(), info.owner());
    }

    @Test
    void listsEveryBusinessAndTheOnesACharacterOwns() {
        service.create("smithy", "crafting", owner, new BlockPosition("world", 1, 64, 1));
        service.create("bakery", "crafting", worker, new BlockPosition("world", 2, 64, 2));
        service.createServerOwned("docks", "crafting", new BlockPosition("world", 3, 64, 3));

        assertEquals(Set.of("smithy", "bakery", "docks"),
                Set.copyOf(directory.all().stream().map(BusinessInfo::id).toList()));
        assertEquals(List.of("smithy"), directory.ownedBy(owner).stream().map(BusinessInfo::id).toList());
        assertEquals(Optional.empty(), directory.find("nowhere"));
    }

    @Test
    void isACopyThatDoesNotFollowLaterChanges() {
        Business business = service.create("smithy", "crafting", owner, new BlockPosition("world", 1, 64, 1));
        BusinessInfo before = directory.find("smithy").orElseThrow();

        service.rename(business, "Renamed");

        assertEquals("smithy", before.displayName());
        assertEquals("Renamed", directory.find("smithy").orElseThrow().displayName());
    }

    @Test
    void followsTheBlockWhenItMovesAndIsEmptyWithoutOne() {
        Business business = service.create("smithy", "crafting", owner, new BlockPosition("world", 1, 64, 1));

        service.move(business, new BlockPosition("nether", -5, 70, 12));
        assertEquals(Optional.of(new BusinessBlock("nether", -5, 70, 12)), directory.find("smithy").orElseThrow().block());

        service.unplace(business);
        assertEquals(Optional.empty(), directory.find("smithy").orElseThrow().block());
    }
}
