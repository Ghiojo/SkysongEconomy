package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.npc.NpcSheetGateway;
import org.skysongdev.skysongEconomy.business.scale.Amount;
import org.skysongdev.skysongEconomy.business.scale.AmountParser;
import org.skysongdev.skysongEconomy.business.scale.Steps;
import org.skysongdev.skysongEconomy.business.scale.StepsParser;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads benefit blocks and progressions. Every stat, skill, proficiency, tier and ability id is
 * checked against SkysongStats and SkysongNPCs here, so a mistake is reported when the file is
 * read instead of when an NPC is created.
 * <pre>
 * stat-points:
 *   amount: "3 + {level}"
 *   eligible: [str, dex, con]          # optional
 * stats: {focus: 1}
 * proficiency-picks:                   # one block, or a list of them
 *   - skill: martial
 *     amount: "2 + {level}"
 *     max-tier: {by: level, 1: proficient, 4: adept}   # or just: proficient
 *     promote-cost: 1                  # optional
 * proficiencies: {perception: proficient}
 * abilities: [brawler]
 * ability-picks: {amount: 1, from: [sentinel, duelist]}
 * at:                                  # progressions only
 *   by: level
 *   3: {abilities: [brawler]}
 * </pre>
 */
public final class BenefitParser {
    /** The key of a progression's steps. */
    public static final String AT = "at";

    private static final Set<String> BENEFIT_KEYS = Set.of("stat-points", "stats", "proficiency-picks", "proficiencies",
            "abilities", "ability-picks");

    private final SkillCatalog skills;
    private final NpcSheetGateway sheets;
    private final TierOrder tiers;

    /**
     * @param skills checks the skills picks name
     * @param sheets checks stats, proficiencies and abilities, and knows the tiers
     */
    public BenefitParser(SkillCatalog skills, NpcSheetGateway sheets) {
        this.skills = skills;
        this.sheets = sheets;
        this.tiers = new TierOrder(sheets.tierIds());
    }

    /** @return the proficiency tiers the parser checks against, lowest first */
    public TierOrder tiers() {
        return tiers;
    }

    /**
     * Reads a progression: a benefit block that may also have {@code at:} steps.
     *
     * @throws ConfigException when a key is unknown or a value can't be read
     */
    public Progression progression(ConfigSection section) {
        checkKeys(section, true);
        Steps<Benefit> steps = section.contains(AT)
                ? StepsParser.parse(section.section(AT), (table, row) -> benefit(table.section(row)))
                : null;
        return new Progression(read(section), steps);
    }

    /**
     * Reads one benefit block.
     *
     * @throws ConfigException when a key is unknown or a value can't be read
     */
    public Benefit benefit(ConfigSection section) {
        checkKeys(section, false);
        return read(section);
    }

    private static void checkKeys(ConfigSection section, boolean steps) {
        for (String key : section.keys()) {
            if (!BENEFIT_KEYS.contains(key) && !(steps && key.equals(AT))) {
                throw section.error(key, "isn't something an NPC can be given: use stat-points, stats, proficiency-picks, "
                        + "proficiencies, abilities or ability-picks" + (steps ? ", or at for steps" : ""));
            }
        }
    }

    private Benefit read(ConfigSection section) {
        return new Benefit(statPoints(section), stats(section), picks(section), proficiencies(section),
                abilities(section, "abilities"), abilityChoices(section));
    }

    private @Nullable Benefit.StatPoints statPoints(ConfigSection parent) {
        if (!parent.contains("stat-points")) {
            return null;
        }
        ConfigSection section = parent.section("stat-points");
        Set<String> eligible = new LinkedHashSet<>();
        for (String stat : section.strings("eligible")) {
            eligible.add(stat(section, "eligible", stat));
        }
        return new Benefit.StatPoints(AmountParser.require(section, "amount"), eligible);
    }

    private Map<String, Amount> stats(ConfigSection parent) {
        Map<String, Amount> stats = new LinkedHashMap<>();
        if (parent.contains("stats")) {
            ConfigSection section = parent.section("stats");
            for (String key : section.keys()) {
                stats.put(stat(section, key, key), AmountParser.require(section, key));
            }
        }
        return stats;
    }

    private String stat(ConfigSection section, String key, String written) {
        String stat = written.strip().toLowerCase(Locale.ROOT);
        if (!sheets.isStat(stat)) {
            throw section.error(key, "names the stat '" + written + "', which SkysongStats doesn't have");
        }
        return stat;
    }

    private List<Benefit.ProficiencyPicks> picks(ConfigSection parent) {
        Object raw = parent.raw("proficiency-picks");
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Map<?, ?>) {
            return List.of(pick(parent.section("proficiency-picks")));
        }
        return parent.sections("proficiency-picks").stream().map(this::pick).toList();
    }

    private Benefit.ProficiencyPicks pick(ConfigSection section) {
        String skill = section.requireString("skill").strip().toLowerCase(Locale.ROOT);
        if (!skills.exists(skill)) {
            throw section.error("skill", "names the skill '" + skill + "', which SkysongStats doesn't have");
        }
        Amount amount = AmountParser.require(section, "amount");
        int promoteCost = section.integer("promote-cost", 0);
        if (promoteCost < 0) {
            throw section.error("promote-cost", "must be 1 or more, or left out for picks that can't be promoted");
        }
        Object maxTier = section.raw("max-tier");
        if (maxTier == null) {
            return new Benefit.ProficiencyPicks(skill, amount, tiers.firstTrained(), null, promoteCost);
        }
        if (!(maxTier instanceof Map<?, ?>)) {
            return new Benefit.ProficiencyPicks(skill, amount, tier(section, "max-tier", maxTier.toString()), null, promoteCost);
        }
        ConfigSection table = section.section("max-tier");
        Steps<String> cap = StepsParser.parse(table, (rows, row) -> tier(rows, row, rows.requireString(row)));
        // Picks are given at the table's lowest tier; promotion takes them up to the tier reached.
        String lowest = cap.rows().values().stream().min(Comparator.comparingInt(tiers::rank)).orElseThrow();
        return new Benefit.ProficiencyPicks(skill, amount, lowest, cap, promoteCost);
    }

    private Map<String, String> proficiencies(ConfigSection parent) {
        Map<String, String> proficiencies = new LinkedHashMap<>();
        if (parent.contains("proficiencies")) {
            ConfigSection section = parent.section("proficiencies");
            for (String key : section.keys()) {
                String proficiency = key.strip().toLowerCase(Locale.ROOT);
                if (!sheets.isProficiency(proficiency)) {
                    throw section.error(key, "isn't a proficiency SkysongStats has");
                }
                proficiencies.put(proficiency, tier(section, key, section.requireString(key)));
            }
        }
        return proficiencies;
    }

    private String tier(ConfigSection section, String key, String written) {
        String tier = written.strip().toLowerCase(Locale.ROOT);
        if (!tiers.contains(tier)) {
            throw section.error(key, "is '" + written + "', which isn't a proficiency tier: use one of "
                    + String.join(", ", tiers.ids()));
        }
        return tier;
    }

    private List<String> abilities(ConfigSection section, String key) {
        Set<String> abilities = new LinkedHashSet<>();
        for (String written : section.strings(key)) {
            String ability = written.strip();
            if (!sheets.isAbility(ability)) {
                throw section.error(key, "names the ability '" + written + "', which SkysongNPCs' abilities.yml doesn't define");
            }
            abilities.add(ability);
        }
        return List.copyOf(abilities);
    }

    private @Nullable Benefit.AbilityChoices abilityChoices(ConfigSection parent) {
        if (!parent.contains("ability-picks")) {
            return null;
        }
        ConfigSection section = parent.section("ability-picks");
        List<String> from = abilities(section, "from");
        if (from.isEmpty()) {
            throw section.error("from", "must list the abilities to choose from");
        }
        return new Benefit.AbilityChoices(AmountParser.require(section, "amount"), from);
    }
}
