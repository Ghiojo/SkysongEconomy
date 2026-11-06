package org.skysongdev.skysongEconomy.nodes.harvest;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * What one harvest costs. On a business-owned node it is only Harvest Tokens; on a public node it
 * is the tier's {@code public-cost}, where every part is charged together, so
 * {@code {downtime: 1, money: 5}} takes both.
 *
 * @param downtime      downtime, from the pool the player spends from
 * @param harvestTokens the character's Harvest Tokens
 * @param money         money from the player's purse
 */
public record HarvestCost(int downtime, int harvestTokens, BigDecimal money) {
    /** Costs nothing at all. */
    public static final HarvestCost FREE = new HarvestCost(0, 0, BigDecimal.ZERO);

    /** @return a cost of only Harvest Tokens, as on business-owned nodes */
    public static HarvestCost tokens(int amount) {
        return new HarvestCost(0, amount, BigDecimal.ZERO);
    }

    public HarvestCost {
        Objects.requireNonNull(money, "money");
        if (downtime < 0 || harvestTokens < 0 || money.signum() < 0) {
            throw new IllegalArgumentException("A public cost can't be negative: " + downtime + ", " + harvestTokens + ", " + money);
        }
        money = money.signum() == 0 ? BigDecimal.ZERO : money.stripTrailingZeros();
    }

    /** @return whether nothing is charged */
    public boolean isFree() {
        return downtime == 0 && harvestTokens == 0 && money.signum() == 0;
    }
}
