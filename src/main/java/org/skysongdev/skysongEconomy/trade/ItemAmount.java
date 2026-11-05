package org.skysongdev.skysongEconomy.trade;

import java.util.Objects;

/**
 * An amount of one Skysong item.
 *
 * @param materialId the item's material id (its file in the {@code items} folder)
 * @param amount     1 or more
 */
public record ItemAmount(String materialId, int amount) {
    public ItemAmount {
        Objects.requireNonNull(materialId, "materialId");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be 1 or more: " + amount);
        }
    }
}
