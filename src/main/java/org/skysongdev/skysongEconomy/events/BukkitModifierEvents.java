package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.prices.PriceModifierChange;
import org.skysongdev.skysongEconomy.api.prices.PriceModifierChangeEvent;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierListener;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.time.Clock;
import java.util.Optional;

/** Fires {@link PriceModifierChangeEvent}. */
public final class BukkitModifierEvents implements ModifierListener {
    private final PluginManager plugins;
    private final Clock clock;

    public BukkitModifierEvents(PluginManager plugins, Clock clock) {
        this.plugins = plugins;
        this.clock = clock;
    }

    @Override
    public void modifierChanged(PriceModifier modifier) {
        // Only the call below, which says what happened, fires the event.
    }

    @Override
    public void modifierChanged(PriceModifierChange.Kind kind, PriceModifier modifier, Optional<PriceModifier> previous) {
        plugins.callEvent(new PriceModifierChangeEvent(PublicTypes.modifierChange(kind, modifier, previous, clock.instant())));
    }
}
