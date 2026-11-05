package org.skysongdev.skysongEconomy.business.training;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code businesses/training.yml}:
 * <pre>{@code
 * duration-minutes: 30
 * downtime-cost: 1
 * rewards:            # by how many times the business picked the skill
 *   1: {xp: 100, proficiency: 1}
 *   2: {xp: 180, proficiency: 2}
 * }</pre>
 */
public final class TrainingSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/training.yml";

    /**
     * @throws ConfigException when a value is out of range or a pick count is not 1 to 5
     */
    public TrainingSettings parse(ConfigSection root) {
        int minutes = root.integer("duration-minutes", 30);
        if (minutes < 1) {
            throw root.error("duration-minutes", "must be 1 or more");
        }
        int cost = root.integer("downtime-cost", 1);
        if (cost < 0) {
            throw root.error("downtime-cost", "must be 0 or more");
        }
        ConfigSection rewards = root.section("rewards");
        Map<Integer, TrainingReward> byPicks = new HashMap<>();
        for (String key : rewards.keys()) {
            ConfigSection row = rewards.section(key);
            int xp = row.integer("xp", 0);
            int proficiency = row.integer("proficiency", 0);
            if (xp < 0) {
                throw row.error("xp", "must be 0 or more");
            }
            if (proficiency < 0) {
                throw row.error("proficiency", "must be 0 or more");
            }
            byPicks.put(pickCount(rewards, key), new TrainingReward(xp, proficiency));
        }
        return new TrainingSettings(Duration.ofMinutes(minutes), cost, byPicks);
    }

    private static int pickCount(ConfigSection rewards, String key) {
        try {
            int picks = Integer.parseInt(key);
            if (picks >= 1 && picks <= Business.MAX_TIER) {
                return picks;
            }
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw rewards.error(key, "is not a pick count: use 1 to " + Business.MAX_TIER);
    }
}
