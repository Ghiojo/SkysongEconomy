package org.skysongdev.skysongEconomy.api.nodes;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * What one harvest cost the player who gathered.
 *
 * @param downtime      downtime spent
 * @param harvestTokens Harvest Tokens spent
 * @param money         money taken from the player's purse; it left the economy
 */
public record HarvestCostInfo(int downtime, int harvestTokens, BigDecimal money) {
    public HarvestCostInfo {
        Objects.requireNonNull(money, "money");
    }
}
