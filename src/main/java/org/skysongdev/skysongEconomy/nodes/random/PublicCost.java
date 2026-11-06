package org.skysongdev.skysongEconomy.nodes.random;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * What one harvest at a tier costs on a public node. Every part is charged together, so
 * {@code {downtime: 1, money: 5}} takes both.
 *
 * @param downtime      downtime, from the pool the player spends from
 * @param harvestTokens the character's Harvest Tokens
 * @param money         money from the player's purse
 */
public record PublicCost(int downtime, int harvestTokens, BigDecimal money) {
    /** Costs nothing at all. */
    public static final PublicCost FREE = new PublicCost(0, 0, BigDecimal.ZERO);

    public PublicCost {
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
