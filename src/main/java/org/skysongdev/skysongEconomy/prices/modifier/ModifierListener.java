package org.skysongdev.skysongEconomy.prices.modifier;

/**
 * Told when a modifier starts or stops changing prices, so what shows a price (such as a Claim
 * sign) can show the new one. Called on the main thread, after the change is in memory.
 */
@FunctionalInterface
public interface ModifierListener {
    /**
     * @param modifier the modifier added, replaced (its new version, with the same type and
     *                 target), removed, or expired
     */
    void modifierChanged(PriceModifier modifier);
}
