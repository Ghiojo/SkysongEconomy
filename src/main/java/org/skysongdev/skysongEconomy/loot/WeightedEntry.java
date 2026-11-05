package org.skysongdev.skysongEconomy.loot;

import java.util.Objects;

/**
 * One line of a {@link LootGroup}.
 *
 * @param weight how likely it is compared to the group's other entries, 1 or more
 */
public record WeightedEntry(LootEntry entry, int weight) {
    public WeightedEntry {
        Objects.requireNonNull(entry, "entry");
        if (weight < 1) {
            throw new IllegalArgumentException("weight must be 1 or more: " + weight);
        }
    }
}
