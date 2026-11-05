package org.skysongdev.skysongEconomy.loot;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * A list of weighted entries to roll on. Weights are whole numbers, never percentages: each
 * entry's chance is its weight divided by the sum of all weights, so they never have to add up to
 * 100.
 *
 * @param id the group's name in its file, or a path like {@code common[2]} for a group written
 *           inline inside another one; used in messages only
 */
public record LootGroup(String id, List<WeightedEntry> entries) {
    public LootGroup {
        Objects.requireNonNull(id, "id");
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("The group " + id + " needs at least one entry");
        }
    }

    /** @return the sum of every entry's weight */
    public int totalWeight() {
        return entries.stream().mapToInt(WeightedEntry::weight).sum();
    }

    /**
     * Picks one entry: adds all weights together, rolls a number below the total and walks the
     * list until it lands. Doesn't go into nested groups, see {@link LootRoller}.
     */
    public LootEntry pick(RandomGenerator random) {
        int ticket = random.nextInt(totalWeight());
        for (WeightedEntry entry : entries) {
            ticket -= entry.weight();
            if (ticket < 0) {
                return entry.entry();
            }
        }
        throw new IllegalStateException("unreachable: the ticket is below the total weight");
    }

    /** @return the chance, from 0 to 1, that one roll on this group lands on {@code entry} */
    public double chance(WeightedEntry entry) {
        return (double) entry.weight() / totalWeight();
    }
}
