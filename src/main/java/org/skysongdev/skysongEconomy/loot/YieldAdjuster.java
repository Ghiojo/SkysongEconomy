package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.trade.IntRange;

/**
 * Changes amounts and roll counts while a {@link LootOutput} is rolled, which is how booster
 * items take effect.
 */
public interface YieldAdjuster {
    /** Changes nothing. */
    YieldAdjuster NONE = new YieldAdjuster() {
        @Override
        public IntRange amount(IntRange amount) {
            return amount;
        }

        @Override
        public int rolls(int rolls) {
            return rolls;
        }
    };

    /** @return the min-max amount to roll for an item whose table says {@code amount} */
    IntRange amount(IntRange amount);

    /** @return how many times to roll a group the table rolls {@code rolls} times; 0 or more */
    int rolls(int rolls);
}
