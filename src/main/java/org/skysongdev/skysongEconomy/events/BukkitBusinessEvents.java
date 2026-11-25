package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.business.BusinessChange;
import org.skysongdev.skysongEconomy.api.business.BusinessChangeEvent;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;

/** Fires {@link BusinessChangeEvent} for every change {@code BusinessService} describes. */
public final class BukkitBusinessEvents implements BusinessChangeListener {
    private final PluginManager plugins;

    public BukkitBusinessEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void described(BusinessChange change) {
        plugins.callEvent(new BusinessChangeEvent(change));
    }
}
