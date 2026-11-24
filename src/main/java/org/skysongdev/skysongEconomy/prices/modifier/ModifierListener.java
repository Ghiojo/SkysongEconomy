package org.skysongdev.skysongEconomy.prices.modifier;

import org.skysongdev.skysongEconomy.api.prices.PriceModifierChange;

import java.util.Optional;

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

    /**
     * The same, with what happened. By default calls {@link #modifierChanged(PriceModifier)}.
     *
     * @param kind     added, replaced, removed or expired
     * @param modifier as in {@link #modifierChanged(PriceModifier)}
     * @param previous the version it replaced; only when {@code kind} is {@code REPLACED}
     */
    default void modifierChanged(PriceModifierChange.Kind kind, PriceModifier modifier, Optional<PriceModifier> previous) {
        modifierChanged(modifier);
    }
}
