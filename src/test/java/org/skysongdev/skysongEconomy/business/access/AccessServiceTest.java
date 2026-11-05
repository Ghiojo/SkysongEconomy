package org.skysongdev.skysongEconomy.business.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;
import org.skysongdev.skysongEconomy.business.Hire;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessServiceTest {
    /** Workers train for 0 and sell; everyone else trains for 10; owners may add 2 groups. */
    private static final AccessDefaults DEFAULTS = new AccessDefaults(
            new AccessGroup(AccessGroup.WORKERS, EnumSet.of(Permission.TRAIN, Permission.SELL_EXPORTS), BigDecimal.ZERO, Set.of()),
            new AccessGroup(AccessGroup.EVERYONE, EnumSet.of(Permission.TRAIN), BigDecimal.TEN, Set.of()),
            2);

    private final CharacterRef owner = character("Owner");
    private final CharacterRef worker = character("Worker");
    private final CharacterRef stranger = character("Stranger");
    private Business business;
    private InMemoryAccessRepository repository;
    private AccessService access;

    @BeforeEach
    void setUp() {
        business = Business.restore(new BusinessSnapshot("smithy", "Smithy", "crafting", 1, owner, null, null,
                List.of(new Hire(worker, Instant.EPOCH)), List.of()));
        repository = new InMemoryAccessRepository();
        access = new AccessService(DEFAULTS, repository);
    }

    private static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    private static String refusal(Runnable action) {
        return assertThrows(BusinessException.class, action::run).messageKey();
    }

    @Test
    void aNewBusinessHasTheBuiltInGroupsFromConfigWithoutSavingThem() {
        assertEquals(List.of(DEFAULTS.workers(), DEFAULTS.everyone()), access.groups(business));
        assertEquals(0, repository.writes);
    }

    @Test
    void workersAreInTheWorkersGroupAndEveryoneIsInEveryone() {
        assertEquals(List.of(AccessGroup.WORKERS, AccessGroup.EVERYONE),
                access.groupsOf(business, worker).stream().map(AccessGroup::id).toList());
        assertEquals(List.of(AccessGroup.EVERYONE),
                access.groupsOf(business, stranger).stream().map(AccessGroup::id).toList());
    }

    @Test
    void anyGroupThatAllowsSomethingIsEnough() {
        assertTrue(access.allows(business, worker, Permission.SELL_EXPORTS), "workers sell");
        assertFalse(access.allows(business, stranger, Permission.SELL_EXPORTS));
        assertFalse(access.allows(business, worker, Permission.ORDER_IMPORTS));
    }

    @Test
    void theOwnerMayDoEverythingAndTrainsForFree() {
        for (Permission permission : Permission.values()) {
            assertTrue(access.allows(business, owner, permission), permission.key());
        }
        assertEquals(Optional.of(BigDecimal.ZERO), access.trainingPrice(business, owner));
    }

    @Test
    void theLowestPriceAmongGroupsThatAllowTrainingWins() {
        assertEquals(0, access.trainingPrice(business, worker).orElseThrow().compareTo(BigDecimal.ZERO));
        assertEquals(0, access.trainingPrice(business, stranger).orElseThrow().compareTo(BigDecimal.TEN));

        access.createGroup(business, "friends");
        access.setTrainingPrice(business, "friends", new BigDecimal("4"));
        access.addMember(business, "friends", stranger);
        assertEquals(0, access.trainingPrice(business, stranger).orElseThrow().compareTo(BigDecimal.TEN),
                "a group that doesn't allow training doesn't set the price");

        access.setPermission(business, "friends", Permission.TRAIN, true);
        assertEquals(0, access.trainingPrice(business, stranger).orElseThrow().compareTo(new BigDecimal("4")));
    }

    @Test
    void nobodyButTheOwnerTrainsWhenNoGroupAllowsIt() {
        access.setPermission(business, AccessGroup.EVERYONE, Permission.TRAIN, false);
        access.setPermission(business, AccessGroup.WORKERS, Permission.TRAIN, false);

        assertEquals(Optional.empty(), access.trainingPrice(business, stranger));
        assertEquals(Optional.empty(), access.trainingPrice(business, worker));
        assertTrue(access.trainingPrice(business, owner).isPresent());
    }

    @Test
    void customGroupsStartEmptyAtTheEveryonePrice() {
        AccessGroup group = access.createGroup(business, "Customers");

        assertEquals("customers", group.id(), "group names are lower case");
        assertEquals(Set.of(), group.permissions());
        assertEquals(DEFAULTS.everyone().trainingPrice(), group.trainingPrice());
        assertEquals(List.of(AccessGroup.WORKERS, AccessGroup.EVERYONE, "customers"),
                access.groups(business).stream().map(AccessGroup::id).toList());
    }

    @Test
    void membersOfCustomGroupsGetTheirPermissions() {
        access.createGroup(business, "customers");
        access.setPermission(business, "customers", Permission.ORDER_IMPORTS, true);
        assertFalse(access.allows(business, stranger, Permission.ORDER_IMPORTS));

        access.addMember(business, "customers", stranger);
        assertTrue(access.allows(business, stranger, Permission.ORDER_IMPORTS));

        access.removeMember(business, "customers", stranger);
        assertFalse(access.allows(business, stranger, Permission.ORDER_IMPORTS));
    }

    @Test
    void refusesBadGroupNamesAndTooManyGroups() {
        assertEquals("business.access.invalid-group-id", refusal(() -> access.createGroup(business, "no spaces")));
        assertEquals("business.access.invalid-group-id", refusal(() -> access.createGroup(business, "x".repeat(33))));
        assertEquals("business.access.group-taken", refusal(() -> access.createGroup(business, "workers")));

        access.createGroup(business, "a");
        assertEquals("business.access.group-taken", refusal(() -> access.createGroup(business, "A")));
        access.createGroup(business, "b");
        assertEquals("business.access.too-many-groups", refusal(() -> access.createGroup(business, "c")));
    }

    @Test
    void builtInGroupsCantBeDeletedOrGivenMembers() {
        assertEquals("business.access.built-in-delete", refusal(() -> access.deleteGroup(business, "everyone")));
        assertEquals("business.access.built-in-members", refusal(() -> access.addMember(business, "workers", stranger)));
        assertEquals("business.access.built-in-members", refusal(() -> access.removeMember(business, "everyone", stranger)));
    }

    @Test
    void refusesMissingGroupsDoubleMembersAndNegativePrices() {
        assertEquals("business.access.no-such-group", refusal(() -> access.group(business, "nope")));
        access.createGroup(business, "deal");
        access.addMember(business, "deal", stranger);

        assertEquals("business.access.already-member", refusal(() -> access.addMember(business, "deal", stranger)));
        assertEquals("business.access.not-member", refusal(() -> access.removeMember(business, "deal", worker)));
        assertEquals("business.access.invalid-price",
                refusal(() -> access.setTrainingPrice(business, "deal", new BigDecimal("-1"))));
    }

    @Test
    void deletingAGroupFreesItsName() {
        access.createGroup(business, "deal");
        access.deleteGroup(business, "deal");

        assertEquals("business.access.no-such-group", refusal(() -> access.group(business, "deal")));
        access.createGroup(business, "deal");
    }

    @Test
    void everyChangeSavesAllGroupsAndSurvivesALoad() {
        access.createGroup(business, "deal");
        access.addMember(business, "deal", stranger);
        access.setPermission(business, "deal", Permission.TRAIN, true);
        access.setTrainingPrice(business, AccessGroup.EVERYONE, new BigDecimal("25"));
        assertEquals(4, repository.writes);
        assertEquals(3, repository.saved.get("smithy").size());

        AccessService reloaded = new AccessService(DEFAULTS, repository);
        reloaded.loadAll();
        assertEquals(access.groups(business), reloaded.groups(business));
        assertTrue(reloaded.allows(business, stranger, Permission.TRAIN));
    }

    @Test
    void aSavedBusinessMissingABuiltInGroupGetsItFromConfig() {
        repository.saved.put("smithy", List.of(new AccessGroup("deal", Set.of(), BigDecimal.ONE, Set.of(stranger))));
        access.loadAll();

        assertEquals(List.of(AccessGroup.WORKERS, AccessGroup.EVERYONE, "deal"),
                access.groups(business).stream().map(AccessGroup::id).toList());
    }

    @Test
    void forgetsTheGroupsOfADeletedBusiness() {
        access.createGroup(business, "deal");
        access.deleted(business);

        assertEquals(List.of(DEFAULTS.workers(), DEFAULTS.everyone()), access.groups(business));
    }
}
