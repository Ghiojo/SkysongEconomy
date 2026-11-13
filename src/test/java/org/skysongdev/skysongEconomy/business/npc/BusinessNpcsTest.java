package org.skysongdev.skysongEconomy.business.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.InMemoryAccessRepository;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents.RemovalCause;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessNpcsTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical", "medical")::contains;
    private static final SlotSource MARTIAL = SlotSource.skill("martial");
    private static final SlotSource ECONOMICAL = SlotSource.skill("economical");
    private static final BlockPosition BLOCK = new BlockPosition("world", 1, 64, 1);
    /** Martial: one NPC per martial pick. Economical: an NPC at 1, 3 and 5 picks. */
    private static final String CONFIG = """
            skills:
              martial:
                slots: "{level}"
                template: guard
              economical:
                slots: {by: level, 1: 1, 3: 2, 5: 3}
                template: clerk
            """;

    private final CharacterRef owner = character("Owner");
    private final CharacterRef worker = character("Worker");
    private final UUID creator = owner.player();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> announced = new ArrayList<>();
    private final TickingClock clock = new TickingClock();
    private FakeSkysongNpcs skysongNpcs;
    private InMemoryBusinessNpcRepository repository;
    private BusinessService businesses;
    private AccessService access;
    private BusinessNpcs npcs;
    private Business business;

    /** A clock that moves on a second every time it is read, so every NPC has its own creation time. */
    private static final class TickingClock extends Clock {
        private Instant now = Instant.parse("2026-11-13T12:00:00Z");

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
        skysongNpcs = new FakeSkysongNpcs();
        skysongNpcs.templates.add("clerk");
        repository = new InMemoryBusinessNpcRepository();
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(
                        new BusinessType("service", "Service", Set.of("martial", "economical", "medical"), true))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), clock);
        access = new AccessService(new AccessDefaults(
                new AccessGroup(AccessGroup.WORKERS, Set.of(), BigDecimal.ZERO, Set.of()),
                new AccessGroup(AccessGroup.EVERYONE, Set.of(), BigDecimal.ZERO, Set.of()), 5),
                new InMemoryAccessRepository());
        npcs = service(CONFIG);
        businesses.addListener(npcs);

        business = businesses.create("barracks", "service", owner, BLOCK);
        businesses.hire(business, worker);
    }

    private BusinessNpcs service(String config) {
        NpcSettings settings = new NpcSettingsParser(SKILLS, warnings::add)
                .parse(YamlFiles.parse(NpcSettingsParser.FILE, new StringReader(config)));
        return new BusinessNpcs(settings, new BusinessNpcs.Dependencies(businesses, access, skysongNpcs), repository,
                new BusinessNpcEvents() {
                    @Override
                    public void created(Business business, BusinessNpc npc) {
                        announced.add("created " + npc.source());
                    }

                    @Override
                    public void removed(Business business, BusinessNpc npc, RemovalCause cause) {
                        announced.add("removed " + npc.source() + " " + cause);
                    }
                }, clock, warnings::add);
    }

    private static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    /** The NPCs the business lost with their slots since this was last asked. */
    private List<BusinessNpc> lost(Business business) {
        return npcs.takeLostSlots(business).stream().map(LostNpc::npc).toList();
    }

    private static String refusal(Runnable action) {
        return assertThrows(BusinessException.class, action::run).messageKey();
    }

    /** Raises the business to that tier and spends every free pick on the skill. */
    private void pick(String skill, int tier) {
        businesses.setTier(business, tier);
        while (business.freePicks() > 0) {
            businesses.pick(business, skill);
        }
    }

    private NpcSlotGroup group(SlotSource source) {
        return npcs.slots(business).stream().filter(group -> group.source().equals(source)).findFirst().orElseThrow();
    }

    @Test
    void aBusinessWithoutAnNpcSkillHasNoSlots() {
        businesses.pick(business, "medical");

        assertFalse(npcs.hasSlots(business));
        assertEquals(List.of(), npcs.slots(business));
        assertEquals("business.npcs.no-such-source", refusal(() -> npcs.create(business, MARTIAL, creator)));
    }

    @Test
    void everyPickOfASkillCountsTowardsItsSlots() {
        pick("martial", 3);

        assertTrue(npcs.hasSlots(business));
        assertEquals(List.of(new NpcSlotGroup(MARTIAL, 3, List.of())), npcs.slots(business));
        assertEquals(3, group(MARTIAL).free());
    }

    @Test
    void twoNpcSkillsHaveSeparateSlots() {
        businesses.setTier(business, 4);
        businesses.pick(business, "martial");
        businesses.pick(business, "economical");
        businesses.pick(business, "economical");
        businesses.pick(business, "economical");

        assertEquals(1, group(MARTIAL).capacity());
        assertEquals(2, group(ECONOMICAL).capacity(), "an NPC at 1 and at 3 economical picks");
    }

    @Test
    void creatingFillsASlotWithAnNpcOfTheSkillsTemplateOwnedByTheOwner() {
        pick("martial", 1);

        BusinessNpc created = npcs.create(business, MARTIAL, creator);

        assertEquals("guard", skysongNpcs.templateOf(created.npc()));
        assertEquals(Optional.of(owner), skysongNpcs.ownerOf(created.npc()));
        assertTrue(skysongNpcs.inSetup(created.npc()));
        assertEquals(List.of(new SpawnPoint.InFrontOf(creator)), skysongNpcs.spawnPoints);
        assertEquals(List.of(created), npcs.of(business));
        assertEquals(Optional.of(created), npcs.find(created.npc()));
        assertEquals(created, repository.saved.get(created.npc()));
        assertEquals(0, group(MARTIAL).free());
        assertEquals(List.of("created skill:martial"), announced);
    }

    @Test
    void aFullSourceCreatesNothing() {
        pick("martial", 1);
        npcs.create(business, MARTIAL, creator);

        assertEquals("business.npcs.no-free-slot", refusal(() -> npcs.create(business, MARTIAL, creator)));
        assertEquals(1, skysongNpcs.all().size());
    }

    @Test
    void whatSkysongNpcsRefusesIsToldAndFillsNoSlot() {
        pick("martial", 1);
        skysongNpcs.freeSpot = false;
        assertEquals("business.npcs.no-spot", refusal(() -> npcs.create(business, MARTIAL, creator)));

        skysongNpcs.freeSpot = true;
        skysongNpcs.templates.remove("guard");
        assertEquals("business.npcs.unknown-template", refusal(() -> npcs.create(business, MARTIAL, creator)));

        assertEquals(List.of(), npcs.of(business));
        assertTrue(repository.saved.isEmpty());
        assertEquals(List.of(), announced);
    }

    @Test
    void npcsCanAppearNextToTheBusinessBlockInstead() {
        npcs = service("spawn-at: business\nspawn-radius: 4\n" + CONFIG);
        pick("martial", 1);

        npcs.create(business, MARTIAL, creator);
        assertEquals(List.of(new SpawnPoint.NextTo(BLOCK, creator)), skysongNpcs.spawnPoints);

        businesses.unplace(business);
        pick("martial", 2);
        assertEquals("business.npcs.no-block", refusal(() -> npcs.create(business, MARTIAL, creator)));
    }

    @Test
    void theOwnerMayCreateAndSoMayAGroupWithManageNpcs() {
        assertTrue(npcs.mayCreate(business, owner));
        assertFalse(npcs.mayCreate(business, worker));

        access.setPermission(business, AccessGroup.WORKERS, Permission.MANAGE_NPCS, true);
        assertTrue(npcs.mayCreate(business, worker));
        assertFalse(npcs.mayCreate(business, character("Stranger")));
    }

    @Test
    void staffDismissAnNpcAndItsSlotIsFreeAgain() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);

        String name = skysongNpcs.name(created.npc()).orElseThrow();

        LostNpc dismissed = npcs.dismiss(business, created.npc());

        assertEquals(created, dismissed.npc());
        assertEquals(name, dismissed.name(), "the name it had, since it can no longer be asked");
        assertFalse(skysongNpcs.exists(created.npc()));
        assertEquals(1, group(MARTIAL).free());
        assertTrue(repository.saved.isEmpty());
        assertEquals("removed skill:martial DISMISSED", announced.get(1));
        assertEquals("business.npcs.not-of-business", refusal(() -> npcs.dismiss(business, created.npc())));
    }

    @Test
    void anNpcOfAnotherBusinessCannotBeDismissedHere() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);
        Business other = businesses.create("armory", "service", owner, new BlockPosition("world", 2, 64, 2));

        assertEquals("business.npcs.not-of-business", refusal(() -> npcs.dismiss(other, created.npc())));
        assertTrue(skysongNpcs.exists(created.npc()));
    }

    @Test
    void aLowerTierRemovesTheNewestNpcsOverTheLimit() {
        pick("martial", 3);
        BusinessNpc first = npcs.create(business, MARTIAL, creator);
        BusinessNpc second = npcs.create(business, MARTIAL, creator);
        BusinessNpc third = npcs.create(business, MARTIAL, creator);

        businesses.setTier(business, 1);

        assertEquals(List.of(first), npcs.of(business));
        assertTrue(skysongNpcs.exists(first.npc()));
        assertFalse(skysongNpcs.exists(second.npc()));
        assertFalse(skysongNpcs.exists(third.npc()));
        assertEquals(List.of(third, second), lost(business), "newest first");
        assertEquals(List.of(), lost(business), "reported once");
        assertTrue(announced.contains("removed skill:martial SLOT_LOST"));
    }

    @Test
    void clearingTheOnlyPickOfASkillRemovesItsNpcs() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);

        businesses.clearPick(business, 1);

        assertFalse(skysongNpcs.exists(created.npc()));
        assertFalse(npcs.hasSlots(business));
        assertEquals(List.of(created), lost(business));
    }

    @Test
    void goingUpRemovesNothingAndOpensSlots() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);

        pick("martial", 2);

        assertEquals(List.of(created), npcs.of(business));
        assertEquals(1, group(MARTIAL).free());
        assertEquals(List.of(), lost(business));
    }

    @Test
    void npcsFollowTheBusinessToItsNewOwnerAndToTheServer() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);
        CharacterRef heir = character("Heir");

        businesses.setOwner(business, heir);
        assertEquals(Optional.of(heir), skysongNpcs.ownerOf(created.npc()));

        businesses.giveToServer(business);
        assertEquals(Optional.empty(), skysongNpcs.ownerOf(created.npc()));
    }

    @Test
    void changesThatLeaveTheOwnerAloneDoNotTouchTheNpc() {
        pick("martial", 1);
        npcs.create(business, MARTIAL, creator);
        int before = skysongNpcs.ownerChanges;

        businesses.rename(business, "The Barracks");
        businesses.fire(business, worker);

        assertEquals(before, skysongNpcs.ownerChanges);
    }

    @Test
    void aDeletedBusinessTakesItsNpcsWithIt() {
        pick("martial", 2);
        BusinessNpc first = npcs.create(business, MARTIAL, creator);
        BusinessNpc second = npcs.create(business, MARTIAL, creator);

        businesses.delete(business);

        assertFalse(skysongNpcs.exists(first.npc()));
        assertFalse(skysongNpcs.exists(second.npc()));
        assertEquals(Optional.empty(), npcs.find(first.npc()));
        assertEquals(2, announced.stream().filter("removed skill:martial BUSINESS_DELETED"::equals).count());
    }

    @Test
    void anNpcRemovedInSkysongNpcsFreesItsSlot() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);

        skysongNpcs.remove(created.npc());
        npcs.removedElsewhere(created.npc());
        npcs.removedElsewhere(UUID.randomUUID());

        assertEquals(1, group(MARTIAL).free());
        assertTrue(repository.saved.isEmpty());
        assertEquals("removed skill:martial REMOVED_ELSEWHERE", announced.get(1));
    }

    @Test
    void loadingKeepsNpcsInCreationOrderAndForgetsTheOnesThatAreGone() {
        pick("martial", 3);
        BusinessNpc first = npcs.create(business, MARTIAL, creator);
        BusinessNpc second = npcs.create(business, MARTIAL, creator);
        BusinessNpc gone = npcs.create(business, MARTIAL, creator);
        skysongNpcs.remove(gone.npc());
        repository.save(new BusinessNpc(UUID.randomUUID(), "closed-shop", MARTIAL, clock.instant()));

        BusinessNpcs reloaded = service(CONFIG);

        assertEquals(2, reloaded.loadAll());
        assertEquals(List.of(first, second), reloaded.of(business));
        assertEquals(Set.of(first.npc(), second.npc()), repository.saved.keySet());
        assertEquals(2, warnings.size());
    }

    @Test
    void loadingAppliesAFileThatNowGivesFewerSlots() {
        pick("martial", 2);
        BusinessNpc first = npcs.create(business, MARTIAL, creator);
        BusinessNpc second = npcs.create(business, MARTIAL, creator);

        BusinessNpcs reloaded = service("skills: {martial: {slots: 1, template: guard}}");

        assertEquals(1, reloaded.loadAll());
        assertEquals(List.of(first), reloaded.of(business));
        assertFalse(skysongNpcs.exists(second.npc()));
    }

    @Test
    void aSkillTakenOutOfTheFileKeepsItsNpcsButGivesNoNewOnes() {
        pick("martial", 2);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);

        BusinessNpcs reloaded = service("skills: {economical: {slots: 1, template: clerk}}");

        assertEquals(1, reloaded.loadAll());
        assertTrue(skysongNpcs.exists(created.npc()), "a typo in the file must not delete NPCs");
        assertEquals(List.of(new NpcSlotGroup(MARTIAL, 1, List.of(created))), reloaded.slots(business));
        assertEquals("business.npcs.no-free-slot", refusal(() -> reloaded.create(business, MARTIAL, creator)));
        assertEquals(1, warnings.size());
        assertEquals(created, reloaded.dismiss(business, created.npc()).npc(), "staff can still dismiss it");
    }

    @Test
    void loadingGivesNpcsBackToTheOwnerIfTheyDrifted() {
        pick("martial", 1);
        BusinessNpc created = npcs.create(business, MARTIAL, creator);
        skysongNpcs.setOwner(created.npc(), character("Someone Else"));

        service(CONFIG).loadAll();

        assertEquals(Optional.of(owner), skysongNpcs.ownerOf(created.npc()));
    }
}
