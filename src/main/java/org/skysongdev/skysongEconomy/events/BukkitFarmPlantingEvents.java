package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.nodes.FarmPlantingChangeEvent;
import org.skysongdev.skysongEconomy.api.nodes.PlantingChange;
import org.skysongdev.skysongEconomy.items.ItemStackBytes;
import org.skysongdev.skysongEconomy.nodes.farm.FarmPlantingEvents;
import org.skysongdev.skysongEconomy.nodes.farm.Planting;

/** Fires {@link FarmPlantingChangeEvent}. */
public final class BukkitFarmPlantingEvents implements FarmPlantingEvents {
    private final PluginManager plugins;

    public BukkitFarmPlantingEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void changed(PlantingChange change, Planting planting) {
        plugins.callEvent(new FarmPlantingChangeEvent(change, ItemStackBytes.restore(planting.primer())));
    }
}
