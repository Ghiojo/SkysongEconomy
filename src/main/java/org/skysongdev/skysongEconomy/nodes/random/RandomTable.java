package org.skysongdev.skysongEconomy.nodes.random;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A random node table, from {@code nodes/random/<id>.yml}. Many nodes can use one table. A node
 * offers the table's tiers up to its own tier.
 *
 * @param id    the file name, which nodes refer to
 * @param name  what players see, in MiniMessage
 * @param type  what booster items match against, such as {@code mining}
 * @param skill the SkysongStats skill each tier's level is checked in
 * @param tiers the tiers the file lists, by tier number; at least one
 */
public record RandomTable(String id, String name, String type, String skill, Map<Integer, RandomTier> tiers) {
    public RandomTable {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(skill, "skill");
        tiers = new TreeMap<>(tiers);
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("The table " + id + " needs at least one tier");
        }
        tiers.forEach((number, tier) -> {
            if (number != tier.tier()) {
                throw new IllegalArgumentException("Tier " + tier.tier() + " is filed as tier " + number);
            }
        });
        tiers = Collections.unmodifiableMap(tiers);
    }

    /** @return that tier, empty when the file doesn't list it */
    public Optional<RandomTier> tier(int tier) {
        return Optional.ofNullable(tiers.get(tier));
    }

    /** @return the tiers a node of {@code nodeTier} offers, lowest first */
    public List<RandomTier> tiersUpTo(int nodeTier) {
        return tiers.values().stream().filter(tier -> tier.tier() <= nodeTier).toList();
    }
}
