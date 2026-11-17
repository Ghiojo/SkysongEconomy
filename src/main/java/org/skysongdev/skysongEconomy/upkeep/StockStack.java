package org.skysongdev.skysongEconomy.upkeep;

import org.jetbrains.annotations.Nullable;

/**
 * One stack in an NPC's upkeep stock, as the payment rules see it.
 *
 * @param materialId the SkysongItems {@code material_id}, or {@code null} for an item without one,
 *                   which pays nothing
 * @param amount     how many items the stack holds, 1 or more
 */
public record StockStack(@Nullable String materialId, int amount) {
    public StockStack {
        if (amount < 1) {
            throw new IllegalArgumentException("A stack holds 1 or more items: " + amount);
        }
    }
}
