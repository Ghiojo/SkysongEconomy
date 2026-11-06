package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.config.FileEntry;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Something a farm node grows once a primer is planted, from
 * {@code nodes/farm/resources/<id>.yml}. Only the tiers the file lists exist, so a resource can
 * start above tier 1.
 *
 * @param id     the file name, which farm types and the {@code node_primer} tag refer to
 * @param name   what players see, in MiniMessage
 * @param primer the item planting takes, and how many; an item with the {@code node_primer} tag
 *               set to this resource's id is accepted too
 * @param tiers  the tiers the file lists, by tier number; at least one
 */
public record FarmResource(String id, String name, ItemAmount primer, Map<Integer, FarmTier> tiers) implements FileEntry {
    public FarmResource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(primer, "primer");
        tiers = new TreeMap<>(tiers);
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("The farm resource " + id + " needs at least one tier");
        }
        tiers.forEach((number, tier) -> {
            if (number != tier.tier()) {
                throw new IllegalArgumentException("Tier " + tier.tier() + " is filed as tier " + number);
            }
        });
        tiers = Collections.unmodifiableMap(tiers);
    }

    /** @return that tier, empty when the file doesn't list it */
    public Optional<FarmTier> tier(int tier) {
        return Optional.ofNullable(tiers.get(tier));
    }

    /** @return the tiers a farm node of {@code nodeTier} offers, lowest first; none when the resource starts above it */
    public List<FarmTier> tiersUpTo(int nodeTier) {
        return tiers.values().stream().filter(tier -> tier.tier() <= nodeTier).toList();
    }
}
