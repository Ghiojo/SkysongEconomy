package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

/** Reads the tier numbers that node files use as keys, such as {@code tiers: {1: ..., 2: ...}}. */
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
}
