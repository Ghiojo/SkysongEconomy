package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.Objects;

/** What a roll on a {@link LootGroup} can land on. */
public sealed interface LootEntry {
    /**
     * An item with a fixed or random amount.
     *
     * @param materialId the Skysong item's material id
     * @param amount     how many, each amount in the range equally likely
     */
    record Item(String materialId, IntRange amount) implements LootEntry {
        public Item {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(amount, "amount");
        }
    }

    /** Another group: landing on it rolls once more, inside that group. */
    record Group(LootGroup group) implements LootEntry {
        public Group {
            Objects.requireNonNull(group, "group");
        }
    }

    /** A wasted roll. */
    record Nothing() implements LootEntry {
    }
}
