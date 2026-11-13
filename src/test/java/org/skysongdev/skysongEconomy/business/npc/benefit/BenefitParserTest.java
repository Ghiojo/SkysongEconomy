package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.npc.FakeSkysongNpcs;
import org.skysongdev.skysongEconomy.business.scale.BusinessScale;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenefitParserTest {
    private static final SkillCatalog SKILLS = Set.of("martial", "economical")::contains;

    private final FakeSkysongNpcs skysongNpcs = new FakeSkysongNpcs();
    private final BenefitParser parser = new BenefitParser(SKILLS, skysongNpcs);

    private Progression parse(String yaml) {
        return parser.progression(YamlFiles.parse("npcs.yml", new StringReader(yaml)));
    }

    private Entitlement at(String yaml, int level, int tier) {
        return parse(yaml).at(new BusinessScale(level, tier), parser.tiers());
    }

    private String problem(String yaml) {
        return assertThrows(ConfigException.class, () -> parse(yaml)).getMessage();
    }

    @Test
    void anEmptyBlockGivesNothing() {
        assertTrue(parse("{}").isEmpty());
        assertTrue(at("{}", 3, 3).isEmpty());
    }

    @Test
    void statPointsAndPicksFollowTheSkillsLevel() {
        String yaml = """
                stat-points:
                  amount: "3 + {level}"
                  eligible: [strength, dexterity]
                proficiency-picks:
                  - skill: martial
                    amount: "2 + {level}"
                """;

        Entitlement owed = at(yaml, 3, 5);

        assertEquals(Map.of(Set.of("strength", "dexterity"), 6), owed.points());
        assertEquals(Map.of(PickKind.plain("martial", "proficient"), 5), owed.picks());
    }

    @Test
    void picksMayPromoteOnceTheCapIsAboveTheirTier() {
        String yaml = """
                proficiency-picks:
                  skill: martial
                  amount: 3
                  max-tier: {by: level, 1: proficient, 4: adept}
                  promote-cost: 1
                """;

        assertEquals(Map.of(PickKind.plain("martial", "proficient"), 3), at(yaml, 3, 5).picks());
        assertEquals(Map.of(new PickKind("martial", "proficient", "adept", 1), 3), at(yaml, 4, 5).picks());
    }

    @Test
    void withoutAPromoteCostNothingPromotes() {
        String yaml = """
                proficiency-picks:
                  skill: martial
                  amount: 3
                  max-tier: {by: level, 1: proficient, 4: adept}
                """;

        assertEquals(Map.of(PickKind.plain("martial", "proficient"), 3), at(yaml, 5, 5).picks());
    }

    @Test
    void aSingleMaxTierIsTheTierPicksAreGivenAt() {
        String yaml = """
                proficiency-picks: {skill: martial, amount: 1, max-tier: adept}
                """;

        assertEquals(Map.of(PickKind.plain("martial", "adept"), 1), at(yaml, 1, 1).picks());
    }

    @Test
    void everyStepReachedCountsAndAProficiencyKeepsItsHighestTier() {
        String yaml = """
                stats: {wisdom: 1}
                at:
                  by: level
                  1: {proficiencies: {perception: proficient, survival: proficient}}
                  3: {abilities: [keen-eye]}
                  4: {proficiencies: {perception: adept}, stats: {wisdom: 2}}
                  5: {ability-picks: {amount: 1, from: [pack-mule, haggler]}}
                """;

        Entitlement low = at(yaml, 2, 5);
        Entitlement high = at(yaml, 5, 5);

        assertEquals(Map.of("wisdom", 1), low.stats());
        assertEquals(Map.of("perception", "proficient", "survival", "proficient"), low.proficiencies());
        assertTrue(low.abilities().isEmpty());
        assertEquals(Map.of("wisdom", 3), high.stats());
        assertEquals(Map.of("perception", "adept", "survival", "proficient"), high.proficiencies());
        assertEquals(Set.of("keen-eye"), high.abilities());
        assertEquals(Map.of(List.of("pack-mule", "haggler"), 1), high.abilityChoices());
    }

    @Test
    void stepsCanFollowTheBusinessTier() {
        String yaml = """
                at:
                  by: tier
                  3: {abilities: [keen-eye]}
                """;

        assertTrue(at(yaml, 5, 2).isEmpty());
        assertEquals(Set.of("keen-eye"), at(yaml, 1, 3).abilities());
    }

    @Test
    void amountsOfZeroAreLeftOut() {
        assertTrue(at("stat-points: {amount: \"{level} - 1\"}", 1, 1).isEmpty());
    }

    @Test
    void unknownIdsAreRefusedWhenTheFileIsRead() {
        assertTrue(problem("stats: {luck: 1}").contains("luck"));
        assertTrue(problem("stat-points: {amount: 1, eligible: [luck]}").contains("luck"));
        assertTrue(problem("proficiency-picks: {skill: fishing, amount: 1}").contains("fishing"));
        assertTrue(problem("proficiency-picks: {skill: martial, amount: 1, max-tier: legendary}").contains("legendary"));
        assertTrue(problem("proficiencies: {juggling: adept}").contains("juggling"));
        assertTrue(problem("proficiencies: {perception: legendary}").contains("legendary"));
        assertTrue(problem("abilities: [flying]").contains("flying"));
        assertTrue(problem("ability-picks: {amount: 1, from: [flying]}").contains("flying"));
    }

    @Test
    void mistakesInTheShapeAreRefused() {
        assertTrue(problem("stat-point: {amount: 1}").contains("stat-point"));
        assertTrue(problem("stat-points: {eligible: [strength]}").contains("amount"));
        assertTrue(problem("ability-picks: {amount: 1}").contains("from"));
        assertTrue(problem("proficiency-picks: {skill: martial, amount: 1, promote-cost: -1}").contains("promote-cost"));
        assertTrue(problem("at: {by: level, 2: {at: {by: level, 1: {}}}}").contains("at"), "steps don't nest");
    }
}
