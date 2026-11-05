package org.skysongdev.skysongEconomy.business.tier;

/**
 * What one business tier allows.
 *
 * @param tier       the tier, 1 to 5
 * @param maxWorkers how many workers a business of this tier may hire; the owner is not counted
 */
public record TierSettings(int tier, int maxWorkers) {
    public TierSettings {
        if (maxWorkers < 0) {
            throw new IllegalArgumentException("maxWorkers must not be negative: " + maxWorkers);
        }
    }
}
