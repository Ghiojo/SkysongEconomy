package org.skysongdev.skysongEconomy.api.items;

import java.util.Objects;

/**
 * Some of one SkysongItems item: what a trade, harvest or upkeep cycle moved.
 *
 * @param materialId the SkysongItems material id, such as {@code steel_ingot}
 * @param amount     how many, at least 1
 */
public record ItemCount(String materialId, int amount) {
    public ItemCount {
        Objects.requireNonNull(materialId, "materialId");
        if (amount < 1) {
            throw new IllegalArgumentException("An item count needs an amount of at least 1, not " + amount);
        }
    }
}
