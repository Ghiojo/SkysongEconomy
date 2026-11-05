package org.skysongdev.skysongEconomy.business.tier;

import org.skysongdev.skysongEconomy.business.Business;

import java.util.Map;
import java.util.TreeMap;

/** The settings of every tier from {@link Business#MIN_TIER} to {@link Business#MAX_TIER}. */
public final class BusinessTiers {
    private final Map<Integer, TierSettings> byTier;

    /**
     * @throws IllegalArgumentException when a tier is missing
     */
    public BusinessTiers(Iterable<TierSettings> tiers) {
        Map<Integer, TierSettings> map = new TreeMap<>();
        for (TierSettings settings : tiers) {
            map.put(settings.tier(), settings);
        }
        for (int tier = Business.MIN_TIER; tier <= Business.MAX_TIER; tier++) {
            if (!map.containsKey(tier)) {
                throw new IllegalArgumentException("Tier " + tier + " is missing");
            }
        }
        this.byTier = Map.copyOf(map);
    }

    /**
     * @throws IllegalArgumentException when the tier is outside 1 to 5
     */
    public TierSettings get(int tier) {
        TierSettings settings = byTier.get(tier);
        if (settings == null) {
            throw new IllegalArgumentException("No such tier: " + tier);
        }
        return settings;
    }
}
