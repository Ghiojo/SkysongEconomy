package org.skysongdev.skysongEconomy.upkeep;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A named upkeep preset from {@code npc-upkeep.yml}.
 *
 * @param id    its key under {@code profiles}
 * @param every how long one cycle lasts, in real time
 * @param slots how many slots the NPC's upkeep stock has: 9, 18, 27, 36, 45 or 54
 * @param cost  what every cycle eats; each entry is paid in this order, all or nothing
 */
public record UpkeepProfile(String id, Duration every, int slots, List<UpkeepCost> cost) {
    /** The slot counts a chest can show. */
    public static final List<Integer> SLOT_SIZES = List.of(9, 18, 27, 36, 45, 54);

    public UpkeepProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(every, "every");
        if (every.isNegative() || every.isZero()) {
            throw new IllegalArgumentException("every must be longer than 0: " + every);
        }
        if (!SLOT_SIZES.contains(slots)) {
            throw new IllegalArgumentException("slots must be one of " + SLOT_SIZES + ": " + slots);
        }
        cost = List.copyOf(cost);
        if (cost.isEmpty()) {
            throw new IllegalArgumentException("cost needs at least one entry");
        }
    }
}
