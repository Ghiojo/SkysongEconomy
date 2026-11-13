package org.skysongdev.skysongEconomy.business.module.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.Hire;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.InMemoryAccessRepository;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.InMemoryModulePickRepository;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModuleParser;
import org.skysongdev.skysongEconomy.business.module.ModulePick;
import org.skysongdev.skysongEconomy.business.module.ModuleSettings;
import org.skysongdev.skysongEconomy.business.module.ModuleSettingsParser;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffectParsers;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleWorkerSlots;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongEconomy.business.npc.FakeSkysongNpcs;
import org.skysongdev.skysongEconomy.business.npc.InMemoryBusinessNpcRepository;
import org.skysongdev.skysongEconomy.business.npc.LostNpc;
import org.skysongdev.skysongEconomy.business.npc.NpcPending;
import org.skysongdev.skysongEconomy.business.npc.NpcSettings;
import org.skysongdev.skysongEconomy.business.npc.NpcSettingsParser;
import org.skysongdev.skysongEconomy.business.npc.NpcSlotGroup;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;
import org.skysongdev.skysongEconomy.business.npc.benefit.BenefitParser;
import org.skysongdev.skysongEconomy.business.npc.benefit.InMemoryNpcAppliedRepository;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.config.YamlFiles;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettingsParser;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Module effects end to end, with Agustín's kennel and veteran sergeants examples. */
class ModuleEffectsTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical")::contains;
    private static final SlotSource MARTIAL = SlotSource.skill("martial");
    private static final SlotSource KENNEL = SlotSource.module("kennel");
    private static final String KENNEL_SOURCE = "economy:module:kennel";
    private static final String VETERAN_SOURCE = "economy:module:veteran_sergeants";
    private static final String NPCS = """
            skills:
              martial:
                slots: 3
                template: guard
                progression:
                  stat-points: {amount: 3}
            """;
    private static final String MODULES = """
            skills:
              martial:
                picks: {by: level, 1: 2, 3: 3}
                modules: [kennel, veteran_sergeants, sworn_oath]
            """;
    private static final Map<String, String> FILES = new LinkedHashMap<>();

    static {
        FILES.put("kennel", """
                name: Kennel
                effects:
                  - type: npc-slots
                    amount: 1
                    template: hound
                    progression:
                      stats: {strength: 2}
                      abilities: [keen-eye]
                  - type: npc-benefit
                    target: all
                    from: martial
                    benefit:
                      proficiency-picks:
                        - {skill: martial, amount: 1}
                  - type: worker-slots
                    amount: 1
                """);
        FILES.put("veteran_sergeants", """
                name: Veteran Sergeants
                effects:
                  - type: npc-benefit
                    target: 2
                    from: martial
                    benefit:
                      stats: {strength: 1}
                      stat-points: {amount: 2}
                      ability-picks: {amount: 1, from: [pack-mule, haggler]}
                """);
        FILES.put("sworn_oath", "name: Sworn Oath");
    }

    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Aldric");
    private final List<String> warnings = new ArrayList<>();
    private final FakeSkysongNpcs skysongNpcs = new FakeSkysongNpcs();
    private final InMemoryBusinessNpcRepository npcRepository = new InMemoryBusinessNpcRepository();
    private final InMemoryNpcAppliedRepository appliedRepository = new InMemoryNpcAppliedRepository();
    private final InMemoryModulePickRepository pickRepository = new InMemoryModulePickRepository();
    private final InMemoryModuleTargetRepository targetRepository = new InMemoryModuleTargetRepository();
    private final InMemoryBusinessRepository businessRepository = new InMemoryBusinessRepository();
    private BusinessService businesses;
    private BusinessModules modules;
    private ModuleWorkerSlots workers;
    private BusinessNpcs npcs;
    private NpcProgression progression;
    private ModuleTargets targets;
    private Business business;

    @BeforeEach
    void setUp() {
        skysongNpcs.templates.add("hound");
        start();
        business = businesses.create("barracks", "service", owner, new BlockPosition("world", 1, 64, 1));
        businesses.pick(business, "martial");
    }

    /** Builds every service the way the plugin wires them on start, and loads what was saved. */
    private void start() {
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("service", "Service", Set.of("martial", "economical"), true))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, 1)).toList()),
                businessRepository, Clock.systemUTC());
        businesses.loadAll();
        AccessService access = new AccessService(new AccessDefaults(
                new AccessGroup(AccessGroup.WORKERS, Set.of(), java.math.BigDecimal.ZERO, Set.of()),
                new AccessGroup(AccessGroup.EVERYONE, Set.of(), java.math.BigDecimal.ZERO, Set.of()), 5),
                new InMemoryAccessRepository());
        BenefitParser benefits = new BenefitParser(SKILLS, skysongNpcs);

        ModuleParser parser = new ModuleParser(ModuleEffectParsers.basic().withNpcEffects(benefits, SKILLS), warnings::add);
        List<Module> parsed = new ArrayList<>();
        FILES.forEach((id, yaml) -> parsed.add(parser.parse(id, YamlFiles.parse("modules/" + id + ".yml", new StringReader(yaml)))));
        ModuleSettings moduleSettings = new ModuleSettingsParser(SKILLS, warnings::add)
                .parse(YamlFiles.parse(ModuleSettingsParser.FILE, new StringReader(MODULES)), new FileRegistry<>(parsed));
        modules = new BusinessModules(moduleSettings, businesses, pickRepository, BusinessModuleEvents.NONE,
                Clock.systemUTC(), warnings::add);
        workers = new ModuleWorkerSlots(modules, businesses);
        businesses.addWorkerSlots(workers::extra);
        modules.addListener(workers);
        modules.loadAll();
        businesses.addListener(modules);

        NpcSettings settings = new NpcSettingsParser(SKILLS, benefits, warnings::add)
                .parse(YamlFiles.parse(NpcSettingsParser.FILE, new StringReader(NPCS)));
        npcs = new BusinessNpcs(settings, new BusinessNpcs.Dependencies(businesses, access, skysongNpcs), npcRepository,
                BusinessNpcEvents.NONE, Clock.systemUTC(), warnings::add);
        targets = new ModuleTargets(modules, npcs, targetRepository);
        ModuleNpcRules rules = new ModuleNpcRules(modules, targets);
        npcs.addSlotRules(rules);
        npcs.loadAll();
        targets.loadAll();
        progression = new NpcProgression(settings, new NpcProgression.Dependencies(businesses, npcs, skysongNpcs),
                benefits.tiers(), appliedRepository, (changed, npc, changes) -> { }, warnings::add);
        progression.addSlotRules(rules);
        progression.addBenefitRules(rules);
        progression.loadAll();
        npcs.addListener(progression);
        businesses.addListener(npcs);
        businesses.addListener(progression);
        ModuleNpcLink link = new ModuleNpcLink(npcs, progression, targets);
        modules.addListener(link);
        npcs.addListener(link);
    }

    private UUID create(SlotSource source) {
        UUID npc = npcs.create(business, source, owner.player()).npc();
        skysongNpcs.completeSetup(npc);
        return npc;
    }

    private ModuleShare share() {
        return targets.shares(business).get(0);
    }

    private int strength(UUID npc) {
        return skysongNpcs.modifiers(npc).values().stream().filter(modifier -> modifier.stat().equals("strength"))
                .mapToInt(FakeSkysongNpcs.Modifier::amount).sum();
    }

    @Test
    void aModuleAddsNpcSlotsOfItsOwnKind() {
        assertEquals(List.of(MARTIAL), npcs.slots(business).stream().map(NpcSlotGroup::source).toList());

        modules.pick(business, "martial", "kennel");

        assertEquals(List.of(new NpcSlotGroup(MARTIAL, 3, List.of()), new NpcSlotGroup(KENNEL, 1, List.of())),
                npcs.slots(business));
        UUID hound = create(KENNEL);
        assertEquals("hound", skysongNpcs.templateOf(hound), "the module's template, not the skill's");
        assertEquals(2, strength(hound), "the module's own progression");
        assertTrue(skysongNpcs.hasAbility(hound, "keen-eye"));
        assertEquals(NpcPending.NONE, skysongNpcs.pending(hound), "no martial stat points, and not a martial NPC for the benefit");
        assertThrows(BusinessException.class, () -> create(KENNEL), "one slot");
    }

    @Test
    void aBenefitForAllReachesEveryNpcThatCountsNowAndLater() {
        UUID guard = create(MARTIAL);
        assertEquals(new NpcPending(3, 0, 0), skysongNpcs.pending(guard));

        modules.pick(business, "martial", "kennel");

        assertEquals(new NpcPending(3, 1, 0), skysongNpcs.pending(guard), "one martial pick from the kennel");
        assertEquals(1, skysongNpcs.picks(guard, KENNEL_SOURCE + ":picks").size());
        UUID later = create(MARTIAL);
        assertEquals(new NpcPending(3, 1, 0), skysongNpcs.pending(later));
    }

    @Test
    void aModuleAddsWorkerSlotsAndTakesTheNewestHiresWithItWhenItGoes() {
        CharacterRef first = new CharacterRef(UUID.randomUUID(), "Berta");
        CharacterRef second = new CharacterRef(UUID.randomUUID(), "Cuno");
        businesses.hire(business, first);
        assertThrows(BusinessException.class, () -> businesses.hire(business, second), "the tier allows one worker");

        modules.pick(business, "martial", "kennel");
        assertEquals(2, businesses.maxWorkers(business));
        businesses.hire(business, second);

        modules.clear(business, "martial", 1);

        assertEquals(1, businesses.maxWorkers(business));
        assertEquals(List.of(first), business.workers().stream().map(Hire::character).toList());
        assertEquals(List.of(second), workers.takeLostWorkers(business).stream().map(Hire::character).toList());
        assertEquals(List.of(), workers.takeLostWorkers(business), "reported once");
    }

    @Test
    void removingAModuleRemovesItsNpcsAndTakesBackWhatItGave() {
        UUID guard = create(MARTIAL);
        modules.pick(business, "martial", "kennel");
        UUID hound = create(KENNEL);

        modules.clear(business, "martial", 1);

        assertFalse(skysongNpcs.exists(hound), "the module's own NPC goes with its slot");
        assertEquals(List.of(hound), npcs.takeLostSlots(business).stream().map(LostNpc::npc).map(BusinessNpc::npc).toList());
        assertEquals(new NpcPending(3, 0, 0), skysongNpcs.pending(guard), "the kennel's pick is taken back");
        assertEquals(List.of(), skysongNpcs.picks(guard, KENNEL_SOURCE + ":picks"));
        assertEquals(List.of(MARTIAL), npcs.slots(business).stream().map(NpcSlotGroup::source).toList());
    }

    @Test
    void aNumberedBenefitWaitsForTheOwnerToChoose() {
        UUID first = create(MARTIAL);
        modules.pick(business, "martial", "veteran_sergeants");

        assertEquals(2, targets.open(business));
        assertEquals(List.of(first), targets.eligible(business, share()).stream().map(BusinessNpc::npc).toList());
        assertEquals(0, strength(first), "nothing until it is chosen");

        targets.choose(business, share(), first);
        progression.sync(business, npcs.find(first).orElseThrow());

        assertEquals(1, strength(first));
        assertEquals(new NpcPending(5, 0, 1), skysongNpcs.pending(first), "3 martial points, 2 from the module, one ability to choose");
        assertEquals(1, targets.open(business), "one share waits for a second guard");
        assertEquals(List.of(), targets.eligible(business, share()));
        assertEquals(1, targetRepository.saved.size());
    }

    @Test
    void aChoiceIsPermanentAndOnlyCountingNpcsCanBeChosen() {
        UUID first = create(MARTIAL);
        UUID second = create(MARTIAL);
        UUID third = create(MARTIAL);
        modules.pick(business, "martial", "kennel");
        UUID hound = create(KENNEL);
        modules.pick(business, "martial", "veteran_sergeants");
        ModuleShare share = share();

        BusinessException notMartial = assertThrows(BusinessException.class, () -> targets.choose(business, share, hound));
        assertEquals("business.modules.assign.not-eligible", notMartial.messageKey());
        targets.choose(business, share, first);
        assertThrows(BusinessException.class, () -> targets.choose(business, share, first), "already chosen");
        targets.choose(business, share, second);
        BusinessException full = assertThrows(BusinessException.class, () -> targets.choose(business, share, third));
        assertEquals("business.modules.assign.full", full.messageKey());
        assertEquals(0, targets.open(business));
    }

    @Test
    void aRemovedNpcFreesItsShareForAnother() {
        UUID first = create(MARTIAL);
        UUID second = create(MARTIAL);
        modules.pick(business, "martial", "veteran_sergeants");
        targets.choose(business, share(), first);
        progression.sync(business, npcs.find(first).orElseThrow());
        assertEquals(1, targets.open(business));

        npcs.dismiss(business, first);

        assertEquals(2, targets.open(business));
        assertEquals(List.of(), targetRepository.saved);
        targets.choose(business, share(), second);
        progression.sync(business, npcs.find(second).orElseThrow());
        assertEquals(1, strength(second));
    }

    @Test
    void removingTheModuleTakesTheChosenNpcsBenefitBack() {
        UUID guard = create(MARTIAL);
        modules.pick(business, "martial", "veteran_sergeants");
        targets.choose(business, share(), guard);
        progression.sync(business, npcs.find(guard).orElseThrow());
        skysongNpcs.spendPoints(guard, VETERAN_SOURCE, 2);

        modules.clear(business, "martial", 1);

        assertEquals(0, strength(guard));
        assertEquals(new NpcPending(3, 0, 0), skysongNpcs.pending(guard), "spent module points are respecced and taken");
        assertEquals(List.of(), skysongNpcs.points(guard, VETERAN_SOURCE));
        assertEquals(List.of(), targetRepository.saved);
        assertEquals(0, targets.open(business));
    }

    @Test
    void aLowerTierDropsTheNewestModuleWithEverythingItGave() {
        businesses.setTier(business, 3);
        businesses.pick(business, "martial");
        businesses.pick(business, "martial");
        modules.pick(business, "martial", "sworn_oath");
        modules.pick(business, "martial", "veteran_sergeants");
        modules.pick(business, "martial", "kennel");
        UUID guard = create(MARTIAL);
        UUID hound = create(KENNEL);
        businesses.hire(business, new CharacterRef(UUID.randomUUID(), "Berta"));
        businesses.hire(business, new CharacterRef(UUID.randomUUID(), "Cuno"));

        businesses.setTier(business, 2);

        assertEquals(List.of("sworn_oath", "veteran_sergeants"), modules.of(business).stream().map(ModulePick::moduleId).toList());
        assertFalse(skysongNpcs.exists(hound));
        assertEquals(new NpcPending(3, 0, 0), skysongNpcs.pending(guard));
        assertEquals(1, business.workers().size());
        assertEquals(1, workers.takeLostWorkers(business).size());
        assertEquals(List.of(), warnings);
    }

    @Test
    void everythingSurvivesARestartWithoutBeingGivenTwice() {
        UUID guard = create(MARTIAL);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "veteran_sergeants");
        UUID hound = create(KENNEL);
        targets.choose(business, share(), guard);
        progression.sync(business, npcs.find(guard).orElseThrow());
        NpcPending guardBefore = skysongNpcs.pending(guard);

        start();
        business = businesses.require("barracks");

        assertEquals(2, npcs.of(business).size(), "the hound is still a business NPC");
        assertEquals(guardBefore, skysongNpcs.pending(guard));
        assertEquals(1, strength(guard));
        assertEquals(2, strength(hound));
        assertEquals(1, targets.open(business));
        assertTrue(targets.isChosen(modules.of(business).get(1), 0, guard));
        assertEquals(2, businesses.maxWorkers(business));
        assertEquals(List.of(), warnings);
    }

    @Test
    void deletingTheBusinessLeavesNothingBehind() {
        modules.pick(business, "martial", "kennel");
        UUID hound = create(KENNEL);

        businesses.delete(business);

        assertFalse(skysongNpcs.exists(hound));
        assertEquals(List.of(), warnings);
    }

    @Test
    void effectsAreChecked() {
        ModuleEffectParsers parsers = ModuleEffectParsers.basic().withNpcEffects(new BenefitParser(SKILLS, skysongNpcs), SKILLS);

        assertTrue(error(parsers, "effects: [{type: fly}]").contains("isn't an effect"));
        assertTrue(error(parsers, "effects: [{type: worker-slots}]").contains("amount"));
        assertTrue(error(parsers, "effects: [{type: npc-slots, amount: 1}]").contains("template"));
        assertTrue(error(parsers, """
                effects:
                  - {type: npc-slots, amount: 1, template: hound}
                  - {type: npc-slots, amount: 1, template: guard}
                """).contains("more than one"));
        assertTrue(error(parsers, "effects: [{type: npc-benefit, from: module, benefit: {stats: {strength: 1}}}]")
                .contains("from: module"));
        assertTrue(error(parsers, "effects: [{type: npc-benefit, target: 0, benefit: {stats: {strength: 1}}}]").contains("target"));
        assertTrue(error(parsers, "effects: [{type: npc-benefit, from: cooking, benefit: {stats: {strength: 1}}}]").contains("cooking"));
        assertTrue(error(parsers, "effects: [{type: npc-benefit, benefit: {stats: {luck: 1}}}]").contains("luck"));
        assertTrue(error(parsers, "effects: [{type: npc-benefit}]").contains("benefit"));
    }

    private static String error(ModuleEffectParsers parsers, String yaml) {
        return assertThrows(ConfigException.class, () -> parsers.parse("x",
                YamlFiles.parse("modules/x.yml", new StringReader(yaml)))).getMessage();
    }

    @Test
    void withoutSkysongNpcsTheNpcEffectsLoadButDoNothing() {
        ModuleParser parser = new ModuleParser(ModuleEffectParsers.basic().withoutNpcEffects(), warnings::add);

        Module kennel = parser.parse("kennel", YamlFiles.parse("modules/kennel.yml", new StringReader(FILES.get("kennel"))));

        assertEquals(1, kennel.effects(ModuleEffect.WorkerSlots.class).size(), "worker slots still work");
        assertEquals(2, kennel.effects(ModuleEffect.Unavailable.class).size());
        assertEquals(2, warnings.size(), "one warning per effect type");
        assertTrue(warnings.get(0).contains("SkysongNPCs"), warnings.get(0));
    }

    /** One skill can have NPCs, modules and Economy and Logistics at once: each file is read on its own. */
    @Test
    void aSkillCanHaveNpcsModulesAndImportsExportsAtOnce() throws Exception {
        SkillCatalog skills = Set.of("economical")::contains;
        EconomyLogisticsSettings logistics;
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/" + EconomyLogisticsSettingsParser.FILE)), StandardCharsets.UTF_8)) {
            logistics = new EconomyLogisticsSettingsParser(skills, warnings::add)
                    .parse(YamlFiles.parse(EconomyLogisticsSettingsParser.FILE, reader));
        }
        NpcSettings npcSettings = new NpcSettingsParser(skills, new BenefitParser(skills, skysongNpcs), warnings::add)
                .parse(YamlFiles.parse(NpcSettingsParser.FILE, new StringReader("skills: {economical: {slots: 1, template: guard}}")));
        ModuleSettings moduleSettings = new ModuleSettingsParser(skills, warnings::add).parse(
                YamlFiles.parse(ModuleSettingsParser.FILE, new StringReader("skills: {economical: {picks: 1, modules: []}}")),
                FileRegistry.empty());
        businesses.setTier(business, 2);
        businesses.pick(business, "economical");

        assertTrue(logistics.enabled(business), "imports and exports");
        assertEquals(Map.of(SlotSource.skill("economical"), 1), npcSettings.capacities(business), "an NPC slot");
        assertEquals(Map.of("economical", 1), moduleSettings.totals(business), "a module pick");
        assertEquals(List.of(), warnings);
    }

    @Test
    void aBenefitSourceNamesItsModule() {
        assertEquals(Optional.of("kennel"), ModuleNpcRules.moduleOf(ModuleNpcRules.source("kennel")));
        assertEquals(Optional.empty(), ModuleNpcRules.moduleOf("economy:skill:martial"));
        assertEquals(Optional.empty(), ModuleNpcRules.moduleOf(ModuleNpcRules.source("x").replace("x", "")));
    }
}
