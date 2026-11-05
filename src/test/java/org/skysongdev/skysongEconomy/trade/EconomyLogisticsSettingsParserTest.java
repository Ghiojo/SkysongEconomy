package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessSnapshot;
import org.skysongdev.skysongEconomy.business.SkillPick;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyLogisticsSettingsParserTest {
    private final List<String> warnings = new ArrayList<>();
    private final EconomyLogisticsSettingsParser parser =
            new EconomyLogisticsSettingsParser(Set.of("economical", "trading")::contains, warnings::add);

    private EconomyLogisticsSettings parse(String yaml) {
        return parser.parse(YamlFiles.parse(EconomyLogisticsSettingsParser.FILE, new StringReader(yaml)));
    }

    private static List<SkillPick> picks(String... skills) {
        List<SkillPick> picks = new ArrayList<>();
        for (String skill : skills) {
            picks.add(new SkillPick(skill, Instant.EPOCH));
        }
        return picks;
    }

    @Test
    void bundledFileLoads() throws Exception {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/" + EconomyLogisticsSettingsParser.FILE)), StandardCharsets.UTF_8)) {
            EconomyLogisticsSettings settings = parser.parse(YamlFiles.parse(EconomyLogisticsSettingsParser.FILE, reader));
            assertEquals(Set.of("economical"), settings.skills());
            assertEquals(List.of("express", "fast", "regular", "standard", "slow"), List.copyOf(settings.deliveryTimes().keySet()));
            assertEquals(Duration.ofHours(168), settings.deliveryTime("slow").orElseThrow().duration());
            assertEquals(5, settings.maxLevel());
            assertEquals(0, new BigDecimal("50").compareTo(settings.ledgerRemovalFine()));
            assertEquals(List.of(), warnings);
        }
    }

    @Test
    void levelCountsPicksOfAnyEconomyLogisticsSkill() {
        EconomyLogisticsSettings settings = parse("""
                skills: [economical, trading]
                slots: {1: {imports: 1, ledgers: 1}}
                delivery-times: {express: {hours: 24}}
                """);
        assertEquals(0, settings.level(picks("smithing")));
        assertEquals(3, settings.level(picks("economical", "smithing", "trading", "economical")));
    }

    @Test
    void limitsFallBackToTheHighestConfiguredLevel() {
        EconomyLogisticsSettings settings = parse("""
                slots:
                  1: {imports: 1, ledgers: 1}
                  3: {imports: 4, ledgers: 2}
                delivery-times: {express: {hours: 24}}
                """);
        assertEquals(new SlotLimits(1, 1), limitsAt(settings, 2));
        assertEquals(new SlotLimits(4, 2), limitsAt(settings, 5));
        assertEquals(SlotLimits.NONE, limitsAt(settings, 0));
    }

    private static SlotLimits limitsAt(EconomyLogisticsSettings settings, int level) {
        String[] skills = new String[level];
        Arrays.fill(skills, "economical");
        Business business = Business.restore(new BusinessSnapshot("shop", "Shop", "trading", 5,
                new CharacterRef(UUID.randomUUID(), "main"), null, null, List.of(), picks(skills)));
        return settings.limits(business);
    }

    @Test
    void warnsAboutUnknownSkills() {
        parse("""
                skills: [economical, haggling]
                slots: {1: {imports: 1, ledgers: 1}}
                delivery-times: {express: {hours: 24}}
                """);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("haggling"));
    }

    @Test
    void refusesBrokenValues() {
        String times = "delivery-times: {express: {hours: 24}}\n";
        String slots = "slots: {1: {imports: 1, ledgers: 1}}\n";
        for (String bad : List.of(
                times,                                              // no slots
                slots,                                              // no delivery times
                slots + "delivery-times: {express: {hours: 0}}",
                slots + "delivery-times: {Express: {hours: 2}}",
                "slots: {6: {imports: 1}}\n" + times,
                "slots: {1: {imports: -1}}\n" + times,
                slots + times + "ledger-removal-fine: -5",
                slots + times + "skills: []")) {
            assertThrows(ConfigException.class, () -> parse(bad), bad);
        }
    }
}
