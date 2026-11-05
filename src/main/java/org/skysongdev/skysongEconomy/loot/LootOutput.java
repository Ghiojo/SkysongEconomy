package org.skysongdev.skysongEconomy.loot;

import java.util.List;

/**
 * Everything one harvest can give, in any mix.
 *
 * @param guaranteed items always given
 * @param chances    items given or not, each on its own
 * @param rolls      rolls on weighted groups
 */
public record LootOutput(List<LootEntry.Item> guaranteed, List<ChanceItem> chances, List<GroupRolls> rolls) {
    public LootOutput {
        guaranteed = List.copyOf(guaranteed);
        chances = List.copyOf(chances);
        rolls = List.copyOf(rolls);
        if (guaranteed.isEmpty() && chances.isEmpty() && rolls.isEmpty()) {
            throw new IllegalArgumentException("An output needs at least one item, chance or roll");
        }
    }
}
