package org.skysongdev.skysongEconomy.api.piers;

import java.util.Objects;

/**
 * Some of one item in a shipment.
 *
 * @param materialId the SkysongItems material id, such as {@code steel_sword}
 * @param amount     how many, at least 1
 */
public record ShipmentItem(String materialId, int amount) {
    public ShipmentItem {
        Objects.requireNonNull(materialId, "materialId");
        if (amount < 1) {
            throw new IllegalArgumentException("A shipment item needs an amount of at least 1, not " + amount);
        }
    }
}
