package org.skysongdev.skysongEconomy.business.npc.access;

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
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.FakeSkysongNpcs;
import org.skysongdev.skysongEconomy.business.npc.InMemoryBusinessNpcRepository;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSettingsParser;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.SpawnPoint;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitParser;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessNpcAccessTest {
    private static final SkillCatalog SKILLS = Set.of("martial")::contains;
    private static final SlotSource MARTIAL = SlotSource.skill("martial");
    private static final String CONFIG = """
            default-access:
              workers: [viewer, teleport]
              everyone: []
            skills:
              martial:
                slots: 3
                template: guard
            """;
    private static final String POLICY = "economy.barracks";
    private static final String WORKERS = "economy:barracks/workers";

    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Aldric");
    private final List<String> warnings = new ArrayList<>();
    private final FakeSkysongNpcs skysongNpcs = new FakeSkysongNpcs();
    private final InMemoryBusinessNpcRepository npcRepository = new InMemoryBusinessNpcRepository();
    private BusinessService businesses;
    private AccessService access;
    private BusinessNpcs npcs;
    private BusinessNpcAccess npcAccess;
    private Business business;

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
        start();
        business = businesses.create("barracks", "service", owner, new BlockPosition("world", 1, 64, 1));
        businesses.pick(business, "martial");
    }

    /** Builds the services the way the plugin wires them on start. */
    private void start() {
        ConfigSection root = YamlFiles.parse(NpcSettingsParser.FILE, new StringReader(CONFIG));
        NpcSettings settings = new NpcSettingsParser(SKILLS, new BenefitParser(SKILLS, skysongNpcs), warnings::add).parse(root);
        npcs = new BusinessNpcs(settings, new BusinessNpcs.Dependencies(businesses, access, skysongNpcs), npcRepository,
                BusinessNpcEvents.NONE, Clock.systemUTC(), warnings::add);
        npcAccess = new BusinessNpcAccess(NpcAccessDefaults.parse(root, new NpcActions(skysongNpcs)),
                new BusinessNpcAccess.Dependencies(businesses, access, npcs, skysongNpcs), warnings::add);
        npcs.addListener(npcAccess);
        businesses.addListener(npcs);
        businesses.addListener(npcAccess);
        access.onGroupDeleted(npcAccess::groupDeleted);
    }

    private UUID create() {
        return npcs.create(business, MARTIAL, owner.player()).npc();
    }

    @Test
    void aNewNpcFollowsItsBusinessPolicyWhichStartsWithTheDefaults() {
        UUID npc = create();

        assertEquals(Optional.of(POLICY), skysongNpcs.policyOf(npc));
        assertEquals(Map.of(WORKERS, Set.of("info", "stats-view", "teleport")), skysongNpcs.grants(POLICY),
                "the viewer preset and one more action for workers, nothing for everyone");
    }

    @Test
    void everyNpcOfTheBusinessSharesOnePolicy() {
        UUID first = create();
        npcAccess.set(business, "everyone", "info", true);
        UUID second = create();

        assertEquals(skysongNpcs.policyOf(first), skysongNpcs.policyOf(second));
        assertEquals(Set.of("info"), npcAccess.allowed(business, "everyone"), "the defaults are applied only once");
    }

    @Test
    void grantsListEveryGroupOfTheBusinessInOrder() {
        access.createGroup(business, "guards");

        Map<String, Set<String>> grants = npcAccess.grants(business);

        assertEquals(List.of("workers", "everyone", "guards"), List.copyOf(grants.keySet()));
        assertEquals(Set.of("info", "stats-view", "teleport"), grants.get("workers"));
        assertEquals(Set.of(), grants.get("guards"));
    }

    @Test
    void allowingAndDenyingChangesOneGroupOnly() {
        access.createGroup(business, "guards");

        assertEquals(Set.of("follow", "look"), Set.copyOf(setAll("guards", true, "follow", "look")));
        assertEquals(Set.of("look"), npcAccess.set(business, "guards", "follow", false));

        assertEquals(Set.of("look"), skysongNpcs.grants(POLICY).get("economy:barracks/guards"));
        assertEquals(Set.of("info", "stats-view", "teleport"), npcAccess.allowed(business, "workers"));
    }

    private Set<String> setAll(String group, boolean allowed, String... actions) {
        Set<String> now = Set.of();
        for (String action : actions) {
            now = npcAccess.set(business, group, action, allowed);
        }
        return now;
    }

    @Test
    void aPresetAllowsOrDeniesEverythingItStandsFor() {
        assertEquals(Set.of("info", "stats-view"), npcAccess.set(business, "everyone", "Viewer", true));
        assertEquals(Set.of("teleport"), npcAccess.set(business, "workers", "viewer", false));
    }

    @Test
    void togglingFlipsOneAction() {
        assertTrue(npcAccess.toggle(business, "everyone", "follow"));
        assertFalse(npcAccess.toggle(business, "everyone", "follow"));
        assertEquals(Set.of(), npcAccess.allowed(business, "everyone"));
    }

    @Test
    void unknownActionsAndGroupsAreRefused() {
        BusinessException action = assertThrows(BusinessException.class,
                () -> npcAccess.set(business, "workers", "fly", true));
        assertEquals("business.npcs.access.unknown-action", action.messageKey());
        assertThrows(BusinessException.class, () -> npcAccess.set(business, "nobody", "info", true));
        assertThrows(BusinessException.class, () -> npcAccess.allowed(business, "nobody"));
    }

    @Test
    void aDeletedGroupLosesItsGrantsSoARecreatedOneStartsEmpty() {
        access.createGroup(business, "guards");
        npcAccess.set(business, "guards", "manage", true);

        access.deleteGroup(business, "guards");
        access.createGroup(business, "guards");

        assertEquals(Set.of(), npcAccess.allowed(business, "guards"));
        assertFalse(skysongNpcs.grants(POLICY).containsKey("economy:barracks/guards"));
    }

    @Test
    void deletingABusinessDeletesItsPolicy() {
        create();

        businesses.delete(business);

        assertFalse(skysongNpcs.hasPolicy(POLICY));
    }

    /** @return an NPC no business has */
    private UUID stranger() {
        return skysongNpcs.create("guard", owner, new SpawnPoint.InFrontOf(owner.player()), 3).npc().orElseThrow();
    }

    @Test
    void aCopyOfABusinessNpcStopsFollowingThePolicy() {
        UUID npc = create();
        UUID copy = stranger();
        // SkysongNPCs makes a copy follow its original's policy.
        skysongNpcs.assign(copy, POLICY);

        npcAccess.copied(npc, copy);

        assertEquals(Optional.empty(), skysongNpcs.policyOf(copy));
        assertEquals(Optional.of(POLICY), skysongNpcs.policyOf(npc));
    }

    @Test
    void aCopyOfAnotherNpcKeepsItsOwnPolicy() {
        UUID stranger = stranger();
        skysongNpcs.createPolicy("town");
        skysongNpcs.assign(stranger, "town");

        npcAccess.copied(stranger, stranger);

        assertEquals(Optional.of("town"), skysongNpcs.policyOf(stranger));
    }

    @Test
    void loadingPutsNpcsBackOnThePolicyAndDropsGrantsOfGroupsThatAreGone() {
        UUID npc = create();
        UUID other = create();
        skysongNpcs.unassign(npc);
        skysongNpcs.setGrant(POLICY, "economy:barracks/ghosts", Set.of("info"));
        skysongNpcs.setGrant(POLICY, "town:guards", Set.of("info"));

        assertEquals(1, npcAccess.loadAll());

        assertEquals(Optional.of(POLICY), skysongNpcs.policyOf(npc));
        assertEquals(Optional.of(POLICY), skysongNpcs.policyOf(other));
        assertFalse(skysongNpcs.grants(POLICY).containsKey("economy:barracks/ghosts"));
        assertTrue(skysongNpcs.grants(POLICY).containsKey("town:guards"), "what staff added by hand in SkysongNPCs stays");
    }

    @Test
    void aPolicyDeletedBehindOurBackComesBackWithTheDefaults() {
        UUID npc = create();
        skysongNpcs.deletePolicy(POLICY);

        assertEquals(1, npcAccess.sync(business));

        assertEquals(Optional.of(POLICY), skysongNpcs.policyOf(npc));
        assertEquals(Set.of("info", "stats-view", "teleport"), npcAccess.allowed(business, "workers"));
    }

    @Test
    void onlyTheOwnerAndGroupsWithThePermissionMayEdit() {
        CharacterRef worker = new CharacterRef(UUID.randomUUID(), "Berta");
        businesses.hire(business, worker);
        assertTrue(npcAccess.mayEdit(business, owner));
        assertFalse(npcAccess.mayEdit(business, worker));

        access.setPermission(business, AccessGroup.WORKERS, Permission.EDIT_NPC_ACCESS, true);

        assertTrue(npcAccess.mayEdit(business, worker));
    }

    @Test
    void aBusinessWithoutNpcsGetsNoPolicyOnLoad() {
        assertEquals(0, npcAccess.loadAll());

        assertFalse(skysongNpcs.hasPolicy(POLICY));
    }

    @Test
    void defaultsRefuseUnknownGroupsAndActions() {
        NpcActions actions = new NpcActions(skysongNpcs);

        ConfigException group = assertThrows(ConfigException.class, () -> NpcAccessDefaults.parse(
                YamlFiles.parse("npcs.yml", new StringReader("default-access: {guards: [info]}")), actions));
        assertTrue(group.getMessage().contains("built-in"), group.getMessage());
        ConfigException action = assertThrows(ConfigException.class, () -> NpcAccessDefaults.parse(
                YamlFiles.parse("npcs.yml", new StringReader("default-access: {workers: [fly]}")), actions));
        assertTrue(action.getMessage().contains("fly"), action.getMessage());
        assertEquals(NpcAccessDefaults.NONE, NpcAccessDefaults.parse(
                YamlFiles.parse("npcs.yml", new StringReader("spawn-radius: 3")), actions));
    }

    @Test
    void policyIdsFitSkysongNpcsLimitAndStayApart() {
        String longId = "a".repeat(64);
        String other = "a".repeat(63) + "b";

        assertEquals("economy.smithy", PolicyIds.of("smithy"));
        assertEquals(64, PolicyIds.of(longId).length());
        assertEquals(PolicyIds.of(longId), PolicyIds.of(longId));
        assertFalse(PolicyIds.of(longId).equals(PolicyIds.of(other)));
        assertTrue(PolicyIds.of(longId).matches("[a-z0-9_.-]{1,64}"));
    }
}
