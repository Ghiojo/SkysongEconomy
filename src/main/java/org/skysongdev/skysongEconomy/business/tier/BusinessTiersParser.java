package org.skysongdev.skysongEconomy.business.tier;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code businesses/tiers.yml}, which must define every tier from 1 to 5:
 * <pre>{@code
 * tiers:
 *   1: {workers: 2, vault-cut: 1, max-proficiency: proficient}
 *   2: {workers: 3, vault-cut: 1.5, max-proficiency: proficient}
 * }</pre>
 */
public final class BusinessTiersParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/tiers.yml";

    /**
     * @throws ConfigException when a tier is missing, unknown or has a bad value
     */
    public BusinessTiers parse(ConfigSection root) {
        ConfigSection tiers = root.section("tiers");
        List<TierSettings> parsed = new ArrayList<>();
        for (String key : tiers.keys()) {
            int tier = parseTier(tiers, key);
            ConfigSection section = tiers.section(key);
            int workers = section.integer("workers", -1);
            if (workers < 0) {
                throw section.error("workers", "is required and must be 0 or more");
            }
            double vaultCut = section.number("vault-cut", 0);
            if (vaultCut < 0) {
                throw section.error("vault-cut", "must be 0 or more");
            }
            String maxProficiency = section.string("max-proficiency", TierSettings.DEFAULT_MAX_PROFICIENCY);
            parsed.add(new TierSettings(tier, workers, BigDecimal.valueOf(vaultCut).stripTrailingZeros(), maxProficiency));
        }
        for (int tier = Business.MIN_TIER; tier <= Business.MAX_TIER; tier++) {
            if (!tiers.contains(String.valueOf(tier))) {
                throw tiers.error(String.valueOf(tier), "is missing: every tier from 1 to 5 must be defined");
            }
        }
        return new BusinessTiers(parsed);
    }

    private static int parseTier(ConfigSection tiers, String key) {
        try {
            int tier = Integer.parseInt(key);
            if (tier >= Business.MIN_TIER && tier <= Business.MAX_TIER) {
                return tier;
            }
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw tiers.error(key, "is not a tier: use 1 to 5");
    }
}
