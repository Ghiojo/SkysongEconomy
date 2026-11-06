package org.skysongdev.skysongEconomy.nodes.random;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.nodes.TierKeys;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestCost;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Reads one random table file:
 * <pre>{@code
 * name: "<gray>Mine"           # what players see; default the file name
 * type: mining                 # what boosters match; default the file name
 * skill: mining                # the skill each tier's level is checked in
 * tiers:                       # only the tiers listed exist, from 1 to 5
 *   1:
 *     cost: 1                  # Harvest Tokens, on business-owned nodes
 *     public-cost: {downtime: 1, harvest-tokens: 0, money: 5}   # left out: not on public nodes
 *     rolls:
 *       - times: 8
 *         from:
 *           - {group: unskilled-ores, weight: 60}
 *           - {group: novice-ores, weight: 30}
 *     chances:
 *       - {item: geode, chance: 2.5}
 * }</pre>
 * Each tier's output is read by {@link LootParser#parseOutput}, so it also takes
 * {@code guaranteed} and every entry a group file takes.
 */
public final class RandomTableParser {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "nodes/random";
    private static final Set<String> PUBLIC_COST_KEYS = Set.of("downtime", "harvest-tokens", "money");

    private final LootParser loot;
    private final Predicate<String> skillExists;

    /**
     * @param loot        reads each tier's output
     * @param skillExists whether a skill id exists in SkysongStats
     */
    public RandomTableParser(LootParser loot, Predicate<String> skillExists) {
        this.loot = loot;
        this.skillExists = skillExists;
    }

    /**
     * @param id     the file name without {@code .yml}
     * @param groups the loot groups the tiers may roll on
     * @throws ConfigException when anything in the file is missing or wrong
     */
    public RandomTable parse(String id, ConfigSection root, LootGroupLibrary groups) {
        String skill = root.requireString("skill").strip().toLowerCase(Locale.ROOT);
        if (!skillExists.test(skill)) {
            throw root.error("skill", "'" + skill + "' isn't a skill in SkysongStats");
        }
        ConfigSection tierSection = root.section("tiers");
        if (tierSection.keys().isEmpty()) {
            throw root.error("tiers", "must list at least one tier, from 1 to 5");
        }
        Map<Integer, RandomTier> tiers = new TreeMap<>();
        for (String key : tierSection.keys()) {
            int number = TierKeys.parse(tierSection, key);
            if (tiers.containsKey(number)) {
                throw tierSection.error(key, "lists tier " + number + " twice");
            }
            tiers.put(number, tier(number, tierSection.section(key), groups));
        }
        return new RandomTable(id, root.string("name", id), root.string("type", id).strip().toLowerCase(Locale.ROOT),
                skill, tiers);
    }

    private RandomTier tier(int number, ConfigSection section, LootGroupLibrary groups) {
        if (!section.contains("cost")) {
            throw section.error("cost", "is required: the Harvest Tokens one harvest costs");
        }
        int cost = section.integer("cost", 0);
        if (cost < 0) {
            throw section.error("cost", "can't be negative");
        }
        Optional<HarvestCost> publicCost = section.contains("public-cost")
                ? Optional.of(publicCost(section.section("public-cost")))
                : Optional.empty();
        return new RandomTier(number, cost, publicCost, loot.parseOutput(section, groups));
    }

    private static HarvestCost publicCost(ConfigSection section) {
        for (String key : section.keys()) {
            if (!PUBLIC_COST_KEYS.contains(key)) {
                throw section.error(key, "isn't a cost: use downtime, harvest-tokens or money");
            }
        }
        int downtime = section.integer("downtime", 0);
        int tokens = section.integer("harvest-tokens", 0);
        double money = section.number("money", 0);
        if (downtime < 0) {
            throw section.error("downtime", "can't be negative");
        }
        if (tokens < 0) {
            throw section.error("harvest-tokens", "can't be negative");
        }
        if (money < 0) {
            throw section.error("money", "can't be negative");
        }
        return new HarvestCost(downtime, tokens, BigDecimal.valueOf(money));
    }
}
