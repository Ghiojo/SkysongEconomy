package org.skysongdev.skysongEconomy.api.trade;

import java.util.Objects;

/**
 * Some of one item in a trade.
 *
 * @param materialId the SkysongItems material id, such as {@code steel_ingot}
 * @param amount     how many, at least 1
 */
public record TradeItem(String materialId, int amount) {
    public TradeItem {
        Objects.requireNonNull(materialId, "materialId");
        if (amount < 1) {
            throw new IllegalArgumentException("A trade item needs an amount of at least 1, not " + amount);
        }
    }
}
