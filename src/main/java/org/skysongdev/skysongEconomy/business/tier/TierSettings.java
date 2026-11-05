package org.skysongdev.skysongEconomy.business.tier;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * What one business tier allows.
 *
 * @param tier       the tier, 1 to 5
 * @param maxWorkers how many workers a business of this tier may hire; the owner is not counted
 * @param vaultCut   what each finished worker shift adds to the business vault, on top of the
 *                   worker's pay; the owner's own shifts add nothing
 */
public record TierSettings(int tier, int maxWorkers, BigDecimal vaultCut) {
    public TierSettings {
        if (maxWorkers < 0) {
            throw new IllegalArgumentException("maxWorkers must not be negative: " + maxWorkers);
        }
        Objects.requireNonNull(vaultCut, "vaultCut");
        if (vaultCut.signum() < 0) {
            throw new IllegalArgumentException("vaultCut must not be negative: " + vaultCut);
        }
    }

    /** A tier whose shifts add nothing to the vault. */
    public TierSettings(int tier, int maxWorkers) {
        this(tier, maxWorkers, BigDecimal.ZERO);
    }
}
