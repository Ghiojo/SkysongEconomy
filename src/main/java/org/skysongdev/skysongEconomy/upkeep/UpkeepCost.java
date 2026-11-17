package org.skysongdev.skysongEconomy.upkeep;

import java.util.Objects;

/**
 * One entry of a profile's cost: how many items of one kind a cycle eats.
 *
 * @param match  which items count
 * @param amount how many of them, 1 or more
 */
public record UpkeepCost(Match match, int amount) {
    public UpkeepCost {
        Objects.requireNonNull(match, "match");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be 1 or more: " + amount);
        }
    }

    /** Which items pay a cost entry. */
    public sealed interface Match {
        /** One item, by its SkysongItems {@code material_id}: {@code item: water_flask}. */
        record Item(String materialId) implements Match {
            public Item {
                Objects.requireNonNull(materialId, "materialId");
            }
        }

        /** Any item of an item category, a good type set with {@code /se good}: {@code good-type: food}. */
        record GoodType(String goodType) implements Match {
            public GoodType {
                Objects.requireNonNull(goodType, "goodType");
            }
        }
    }
}
