package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * Reads what every node file's tiers share: the tier numbers they use as keys, such as
 * {@code tiers: {1: ..., 2: ...}}, and each tier's Harvest Token {@code cost}.
 */
public final class TierKeys {
    private TierKeys() {
    }

    /**
     * @return the tier {@code key} names
     * @throws ConfigException when it isn't a whole number from 1 to 5
     */
    public static int parse(ConfigSection section, String key) {
        try {
            int tier = Integer.parseInt(key.strip());
            if (tier >= Node.MIN_TIER && tier <= Node.MAX_TIER) {
                return tier;
            }
        } catch (NumberFormatException ignored) {
            // Refused below, like a number out of range.
        }
        throw section.error(key, "must be a tier from " + Node.MIN_TIER + " to " + Node.MAX_TIER);
    }

    /**
     * Reads the {@code tiers} section of a node file, where only the tiers listed exist.
     *
     * @param tier reads one tier from its number and section
     * @return each tier by its number, lowest first
     * @throws ConfigException when no tier is listed, a key isn't a tier, or {@code tier} refuses one
     */
    public static <T> Map<Integer, T> readTiers(ConfigSection root, BiFunction<Integer, ConfigSection, T> tier) {
        ConfigSection tiers = root.section("tiers");
        if (tiers.keys().isEmpty()) {
            throw root.error("tiers", "must list at least one tier, from " + Node.MIN_TIER + " to " + Node.MAX_TIER);
        }
        Map<Integer, T> read = new TreeMap<>();
        for (String key : tiers.keys()) {
            int number = parse(tiers, key);
            if (read.containsKey(number)) {
                throw tiers.error(key, "lists tier " + number + " twice");
            }
            read.put(number, tier.apply(number, tiers.section(key)));
        }
        return read;
    }

    /**
     * @return the tier's {@code cost}: the Harvest Tokens one harvest at it takes
     * @throws ConfigException when it is missing or negative
     */
    public static int cost(ConfigSection tier) {
        if (!tier.contains("cost")) {
            throw tier.error("cost", "is required: the Harvest Tokens one harvest costs");
        }
        int cost = tier.integer("cost", 0);
        if (cost < 0) {
            throw tier.error("cost", "can't be negative");
        }
        return cost;
    }
}
