package org.skysongdev.skysongEconomy.business.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents.RemovalCause;
import org.skysongdev.skysongEconomy.business.module.ModuleOption.Blocked;
import org.skysongdev.skysongEconomy.business.module.effect.ModuleEffectParsers;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessModulesTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical")::contains;
    /** Agustín's example: one module pick at martial level 1, two at 3, three at 5. */
    private static final String CONFIG = """
            skills:
              martial:
                picks: {by: level, 1: 1, 3: 2, 5: 3}
                modules: [kennel, sworn_oath, banner, drill_yard]
              economical:
                picks: 1
                modules: [sworn_oath]
            """;
    private static final Map<String, String> FILES = new LinkedHashMap<>();

    static {
        FILES.put("kennel", "name: Kennel");
        FILES.put("sworn_oath", "{name: Sworn Oath, icon: PAPER, description: Bound by oath.}");
        FILES.put("banner", "{name: Banner, max-picks: 2}");
        FILES.put("drill_yard", "{name: Drill Yard, requires: {tier: 4, level: 3, modules: [kennel]}}");
    }

    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Aldric");
    private final List<String> warnings = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final InMemoryModulePickRepository repository = new InMemoryModulePickRepository();
    private BusinessService businesses;
    private BusinessModules modules;
    private Business business;

    @BeforeEach
    void setUp() {
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("service", "Service", Set.of("martial", "economical"), true))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), Clock.systemUTC());
        start(CONFIG);
        business = businesses.create("barracks", "service", owner, new BlockPosition("world", 1, 64, 1));
    }

    /** Builds the service from the files, the way the plugin wires it on start. */
    private void start(String config) {
        ModuleParser parser = new ModuleParser(ModuleEffectParsers.basic().withoutNpcEffects(), warnings::add);
        List<Module> parsed = new ArrayList<>();
        FILES.forEach((id, yaml) -> parsed.add(parser.parse(id, YamlFiles.parse("modules/" + id + ".yml", new StringReader(yaml)))));
        ModuleSettings settings = new ModuleSettingsParser(SKILLS, warnings::add)
                .parse(YamlFiles.parse(ModuleSettingsParser.FILE, new StringReader(config)), new FileRegistry<>(parsed));
        modules = new BusinessModules(settings, businesses, repository, new BusinessModuleEvents() {
            @Override
            public void picked(Business changed, ModulePick pick) {
                events.add("picked " + pick.moduleId());
            }

            @Override
            public void removed(Business changed, ModulePick pick, RemovalCause cause) {
                events.add(cause + " " + pick.moduleId());
            }
        }, Clock.systemUTC(), warnings::add);
        businesses.addListener(modules);
    }

    /** Raises the business to that tier and spends every free pick on martial. */
    private void martial(int tier) {
        businesses.setTier(business, tier);
        while (business.freePicks() > 0) {
            businesses.pick(business, "martial");
        }
    }

    private List<String> picked() {
        return modules.of(business).stream().map(ModulePick::moduleId).toList();
    }

    @Test
    void aBusinessWithoutAModuleSkillHasNoModulePicks() {
        assertFalse(modules.hasPicks(business));
        assertEquals(List.of(), modules.groups(business));
        assertThrows(BusinessException.class, () -> modules.pick(business, "martial", "kennel"));
    }

    @Test
    void theSkillsLevelDecidesHowManyPicksThereAre() {
        martial(1);
        assertEquals(List.of(new ModulePickGroup("martial", 1, List.of())), modules.groups(business));

        martial(3);
        assertEquals(2, modules.free(business, "martial"));

        martial(5);
        assertEquals(3, modules.free(business, "martial"));
    }

    @Test
    void pickingTakesAFreePickAndIsSaved() {
        martial(1);

        ModulePick pick = modules.pick(business, "martial", "kennel");

        assertEquals(new ModulePick("barracks", 1, "martial", "kennel", pick.pickedAt()), pick);
        assertEquals(0, modules.free(business, "martial"));
        assertTrue(modules.has(business, "kennel"));
        assertEquals(List.of(pick), repository.saved);
        assertEquals(List.of("picked kennel"), events);
        BusinessException full = assertThrows(BusinessException.class, () -> modules.pick(business, "martial", "sworn_oath"));
        assertEquals("business.modules.no-free-pick", full.messageKey());
    }

    @Test
    void onlyModulesOfTheSkillsPoolCanBePicked() {
        businesses.pick(business, "economical");

        BusinessException refused = assertThrows(BusinessException.class, () -> modules.pick(business, "economical", "kennel"));

        assertEquals("business.modules.not-in-pool", refused.messageKey());
        assertEquals("sworn_oath", modules.pick(business, "economical", "sworn_oath").moduleId());
    }

    @Test
    void aModuleIsPickedOncePerBusinessUnlessMaxPicksSaysMore() {
        martial(5);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "banner");

        assertEquals(Blocked.MAX_PICKS, option("kennel").blocked());
        assertNull(option("banner").blocked(), "max-picks: 2");
        BusinessException refused = assertThrows(BusinessException.class, () -> modules.pick(business, "martial", "kennel"));
        assertEquals("business.modules.blocked.max-picks", refused.messageKey());
        modules.pick(business, "martial", "banner");
        assertEquals(List.of("kennel", "banner", "banner"), picked());
    }

    @Test
    void onceCountsAcrossSkills() {
        martial(1);
        businesses.setTier(business, 2);
        businesses.pick(business, "economical");
        modules.pick(business, "martial", "sworn_oath");

        assertEquals(Blocked.MAX_PICKS, modules.pool(business, "economical").get(0).blocked());
    }

    private ModuleOption option(String moduleId) {
        return modules.pool(business, "martial").stream().filter(option -> option.module().id().equals(moduleId))
                .findFirst().orElseThrow();
    }

    @Test
    void requirementsBlockAModuleUntilTheyAreMet() {
        martial(1);
        assertEquals(Blocked.TIER, option("drill_yard").blocked());

        businesses.setTier(business, 4);
        assertEquals(Blocked.LEVEL, option("drill_yard").blocked(), "tier 4 but one martial pick");

        martial(4);
        assertEquals(Blocked.MODULES, option("drill_yard").blocked());
        BusinessException refused = assertThrows(BusinessException.class, () -> modules.pick(business, "martial", "drill_yard"));
        assertEquals("business.modules.blocked.modules", refused.messageKey());
        assertEquals("kennel", refused.placeholders().get("modules"));

        modules.pick(business, "martial", "kennel");
        assertTrue(option("drill_yard").available());
        modules.pick(business, "martial", "drill_yard");
    }

    @Test
    void fewerPicksDropTheNewestModules() {
        martial(5);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "sworn_oath");
        modules.pick(business, "martial", "banner");
        events.clear();

        businesses.setTier(business, 2);

        assertEquals(List.of("kennel"), picked(), "martial level 2 gives one pick");
        assertEquals(List.of("PICK_LOST banner", "PICK_LOST sworn_oath"), events);
        assertEquals(List.of("banner", "sworn_oath"), modules.takeLostPicks(business).stream().map(ModulePick::moduleId).toList());
        assertEquals(List.of(), modules.takeLostPicks(business), "reported once");
        assertEquals(1, repository.saved.size());
    }

    @Test
    void clearingTheLastSkillPickDropsItsModules() {
        martial(1);
        modules.pick(business, "martial", "kennel");

        businesses.clearPick(business, 1);

        assertEquals(List.of(), picked());
        assertFalse(modules.hasPicks(business));
    }

    @Test
    void staffClearAPickByItsNumberWithinTheSkill() {
        martial(3);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "banner");
        events.clear();

        ModulePick cleared = modules.clear(business, "martial", 1);

        assertEquals("kennel", cleared.moduleId());
        assertEquals(List.of("banner"), picked());
        assertEquals(1, modules.free(business, "martial"));
        assertEquals(List.of("CLEARED kennel"), events);
        BusinessException none = assertThrows(BusinessException.class, () -> modules.clear(business, "martial", 2));
        assertEquals("business.modules.no-such-pick", none.messageKey());
    }

    @Test
    void aNewPickAfterAClearedOneIsStillTheNewest() {
        martial(3);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "banner");
        modules.clear(business, "martial", 1);

        ModulePick again = modules.pick(business, "martial", "kennel");

        assertEquals(3, again.order());
        assertEquals(List.of("banner", "kennel"), picked());
    }

    @Test
    void deletingTheBusinessRemovesItsModules() {
        martial(1);
        modules.pick(business, "martial", "kennel");
        events.clear();

        businesses.delete(business);

        assertEquals(List.of("BUSINESS_DELETED kennel"), events);
        assertEquals(List.of(), modules.of(business));
    }

    @Test
    void picksSurviveARestart() {
        martial(3);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "banner");

        start(CONFIG);

        assertEquals(2, modules.loadAll());
        assertEquals(List.of("kennel", "banner"), picked());
        assertEquals(List.of(), warnings);
    }

    @Test
    void aLowerNumberInTheFileDropsTheNewestOnStart() {
        martial(3);
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "martial", "banner");

        start(CONFIG.replace("{by: level, 1: 1, 3: 2, 5: 3}", "1"));
        modules.loadAll();

        assertEquals(List.of("kennel"), picked());
        assertEquals(List.of(), modules.takeLostPicks(business), "nobody to tell on start");
    }

    @Test
    void aSkillOrModuleRemovedFromTheFilesKeepsItsPicks() {
        martial(1);
        businesses.setTier(business, 2);
        businesses.pick(business, "economical");
        modules.pick(business, "martial", "kennel");
        modules.pick(business, "economical", "sworn_oath");
        FILES.remove("sworn_oath");
        try {
            start("""
                    skills:
                      economical:
                        picks: 1
                        modules: [sworn_oath]
                    """);
            modules.loadAll();
        } finally {
            FILES.put("sworn_oath", "{name: Sworn Oath, icon: PAPER, description: Bound by oath.}");
        }

        assertEquals(List.of("kennel", "sworn_oath"), picked());
        assertEquals(List.of(new ModulePickGroup("economical", 1, List.of(modules.of(business).get(1))),
                new ModulePickGroup("martial", 1, List.of(modules.of(business).get(0)))), modules.groups(business));
        assertEquals(3, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("no module 'sworn_oath'"), warnings.get(0));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("is kept as it is")));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("has no file")));
    }

    @Test
    void picksOfADeletedBusinessAreForgottenOnLoad() {
        repository.save(new ModulePick("gone", 1, "martial", "kennel", Clock.systemUTC().instant()));

        assertEquals(0, modules.loadAll());

        assertEquals(List.of(), repository.saved);
    }

    @Test
    void aModuleFileNeedsANameAndSaneNumbers() {
        ModuleParser parser = new ModuleParser(ModuleEffectParsers.basic().withoutNpcEffects(), warnings::add);

        assertThrows(ConfigException.class, () -> parse(parser, "icon: PAPER"));
        assertThrows(ConfigException.class, () -> parse(parser, "{name: A, max-picks: 0}"));
        assertThrows(ConfigException.class, () -> parse(parser, "{name: A, requires: {tier: 9}}"));
        Module module = parse(parser, """
                name: "<white>Sworn Oath"
                description:
                  - one
                  - two
                requires: {modules: [Kennel]}
                """);
        assertEquals(new Module("x", "<white>Sworn Oath", "PAPER", List.of("one", "two"), 1,
                new ModuleRequirements(1, 1, List.of("kennel")), List.of()), module);
    }

    private static Module parse(ModuleParser parser, String yaml) {
        return parser.parse("x", YamlFiles.parse("modules/x.yml", new StringReader(yaml)));
    }

    @Test
    void theSettingsFileNeedsPicksPerSkillAndMayBeEmpty() {
        ModuleSettingsParser parser = new ModuleSettingsParser(SKILLS, warnings::add);

        assertThrows(ConfigException.class, () -> parser.parse(
                YamlFiles.parse("modules.yml", new StringReader("skills: {martial: {modules: []}}")), FileRegistry.empty()));
        assertEquals(Set.of(), parser.parse(YamlFiles.parse("modules.yml", new StringReader("skills: {}")),
                FileRegistry.empty()).skills().skills());
    }
}
