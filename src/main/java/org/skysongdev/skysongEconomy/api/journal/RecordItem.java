package org.skysongdev.skysongEconomy.api.journal;

import java.util.Objects;

/**
 * Items a journal record moved.
 *
 * @param materialId the SkysongItems material id, such as {@code steel_ingot}
 * @param amount     how many, 1 or more
 * @param flow       whether they came into the game, went out of it, or were lost
 */
public record RecordItem(String materialId, int amount, Flow flow) {
    public RecordItem {
        Objects.requireNonNull(materialId, "materialId");
        Objects.requireNonNull(flow, "flow");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be 1 or more: " + amount);
        }
    }

    /** Which way the items went. */
    public enum Flow {
        /** Given to a player: an import delivered, a harvest, a primer coming back. */
        IN,
        /** Taken from a player: an export sold, a primer planted, upkeep eaten. */
        OUT,
        /** Lost on the way: an import seized at its pier. */
        LOST
    }
}
