package org.skysongdev.skysongEconomy.loot;

import java.util.Objects;

/**
 * An item that is given or not on its own, whatever else is rolled.
 *
 * @param percent the chance in percent, above 0 and at most 100
 */
public record ChanceItem(LootEntry.Item item, double percent) {
    public ChanceItem {
        Objects.requireNonNull(item, "item");
        if (!(percent > 0 && percent <= 100)) {
            throw new IllegalArgumentException("chance must be above 0 and at most 100: " + percent);
        }
    }
}
