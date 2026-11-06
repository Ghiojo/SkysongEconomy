package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.Objects;

/**
 * One tier of a farm resource: what a harvest at that tier costs and gives.
 *
 * @param tier   from 1 to 5
 * @param cost   Harvest Tokens per harvest
 * @param output what one harvest gives
 */
public record FarmTier(int tier, int cost, LootOutput output) {
    public FarmTier {
        if (tier < Node.MIN_TIER || tier > Node.MAX_TIER) {
            throw new IllegalArgumentException("tier must be between " + Node.MIN_TIER + " and " + Node.MAX_TIER + ": " + tier);
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost can't be negative: " + cost);
        }
        Objects.requireNonNull(output, "output");
    }
}
