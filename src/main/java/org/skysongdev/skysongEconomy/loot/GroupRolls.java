package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.Objects;

/**
 * A number of rolls on one group, such as "8 rolls on the tier's rarity groups".
 *
 * @param count how many times to roll, each count in the range equally likely
 */
public record GroupRolls(IntRange count, LootGroup group) {
    public GroupRolls {
        Objects.requireNonNull(count, "count");
        Objects.requireNonNull(group, "group");
    }
}
