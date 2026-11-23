package org.skysongdev.skysongEconomy.trade.pier;

import org.skysongdev.skysongEconomy.api.piers.Pier;
import org.skysongdev.skysongEconomy.api.piers.Piers;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.util.Map;
import java.util.Optional;

/**
 * Keeps {@code pier:<id>} price modifiers in line with the piers: new ones must name a pier that
 * exists, and a pier a plugin removes takes its modifiers with it. A pier that disappears from
 * {@code piers.yml} keeps its modifiers, so a typo in the file and a reload don't wipe them.
 */
public final class PierPriceModifiers implements PierChangeListener {
    private final Piers piers;
    private final PriceModifiers modifiers;

    public PierPriceModifiers(Piers piers, PriceModifiers modifiers) {
        this.piers = piers;
        this.modifiers = modifiers;
    }

    /** Registers the check and the clean-up. */
    public static void connect(PierService piers, PriceModifiers modifiers) {
        PierPriceModifiers rules = new PierPriceModifiers(piers, modifiers);
        modifiers.addTargetCheck(rules::checkTarget);
        piers.addListener(rules);
    }

    /** @throws PriceException {@code prices.modifiers.unknown-pier} when a pier target names no pier */
    public void checkTarget(ModifierTarget target) {
        if (target instanceof ModifierTarget.Pier pier && piers.find(pier.pierId()).isEmpty()) {
            throw new PriceException("prices.modifiers.unknown-pier", Map.of("pier", pier.pierId()));
        }
    }

    @Override
    public void changed(Optional<Pier> before, Optional<Pier> after) {
        if (after.isEmpty() && before.isPresent() && !before.get().isFromConfig()) {
            modifiers.removeAll(new ModifierTarget.Pier(before.get().id()));
        }
    }
}
