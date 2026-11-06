package org.skysongdev.skysongEconomy.nodes.random;

import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.Objects;
import java.util.Optional;

/**
 * One tier of a random table: what a harvest at that tier costs and gives.
 *
 * @param tier       from 1 to 5
 * @param cost       Harvest Tokens per harvest on a business-owned node
 * @param publicCost what a harvest costs on a public node; empty when the tier can't be used on
 *                   public nodes
 * @param output     what one harvest gives
 */
public record RandomTier(int tier, int cost, Optional<PublicCost> publicCost, LootOutput output) {
    public RandomTier {
        if (tier < Node.MIN_TIER || tier > Node.MAX_TIER) {
            throw new IllegalArgumentException("tier must be between " + Node.MIN_TIER + " and " + Node.MAX_TIER + ": " + tier);
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost can't be negative: " + cost);
        }
        Objects.requireNonNull(publicCost, "publicCost");
        Objects.requireNonNull(output, "output");
    }
}
