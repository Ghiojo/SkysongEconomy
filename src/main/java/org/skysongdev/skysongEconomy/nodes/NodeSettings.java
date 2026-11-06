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
 * @param menuIcons          the items the node menu shows
 * @param primerRemovalDowntime the downtime taking a primer out of a farm node costs; 0 makes it free
 */
public record NodeSettings(Set<String> ownerBusinessTypes, Map<Integer, String> tierLevels, NameplateSettings nameplate,
                           NodeMenuIcons menuIcons, int primerRemovalDowntime) {
    /** The removal cost used when {@code nodes/settings.yml} doesn't set one. */
    public static final int DEFAULT_PRIMER_REMOVAL_DOWNTIME = 1;

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
        Objects.requireNonNull(menuIcons, "menuIcons");
        if (primerRemovalDowntime < 0) {
            throw new IllegalArgumentException("primerRemovalDowntime can't be negative: " + primerRemovalDowntime);
        }
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
