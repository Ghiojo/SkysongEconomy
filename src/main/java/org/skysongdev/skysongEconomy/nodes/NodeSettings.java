package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.business.display.NameplateSettings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-wide node settings, from {@code nodes/settings.yml}.
 *
 * @param ownerBusinessTypes the business types that may own nodes
 * @param tierLevels         the skill level gathering at each tier needs, for tiers 1 to 5
 * @param nameplate          how the floating name above node blocks looks
 */
public record NodeSettings(Set<String> ownerBusinessTypes, Map<Integer, String> tierLevels, NameplateSettings nameplate) {
    /** The levels used for tiers that {@code nodes/settings.yml} doesn't list, tier 1 first. */
    public static final List<String> DEFAULT_TIER_LEVELS = List.of("unskilled", "novice", "competent", "journeyman", "expert");

    public NodeSettings {
        ownerBusinessTypes = Set.copyOf(ownerBusinessTypes);
        tierLevels = Map.copyOf(tierLevels);
        for (int tier = Node.MIN_TIER; tier <= Node.MAX_TIER; tier++) {
            if (!tierLevels.containsKey(tier)) {
                throw new IllegalArgumentException("No skill level for tier " + tier);
            }
        }
        Objects.requireNonNull(nameplate, "nameplate");
    }

    /** @return the skill level gathering at {@code tier} needs */
    public String levelFor(int tier) {
        String level = tierLevels.get(tier);
        if (level == null) {
            throw new IllegalArgumentException("tier must be between " + Node.MIN_TIER + " and " + Node.MAX_TIER + ": " + tier);
        }
        return level;
    }
}
