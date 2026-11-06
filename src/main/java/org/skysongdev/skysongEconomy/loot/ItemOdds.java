package org.skysongdev.skysongEconomy.loot;

import java.util.Objects;

/**
 * How likely one item is in one harvest, see {@link LootOdds}.
 *
 * @param materialId    the Skysong item's material id
 * @param chance        the chance, from 0 to 1, of getting at least one
 * @param averageAmount how many come on average, counting the harvests that give none
 */
public record ItemOdds(String materialId, double chance, double averageAmount) {
    public ItemOdds {
        Objects.requireNonNull(materialId, "materialId");
    }
}
