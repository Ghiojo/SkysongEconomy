package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepChangeEvent;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents;

/** Fires {@link NpcUpkeepChangeEvent}. */
public final class BukkitUpkeepEvents implements UpkeepEvents {
    private final PluginManager plugins;

    public BukkitUpkeepEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void stateChanged(NpcUpkeep upkeep, Cause cause) {
        plugins.callEvent(new NpcUpkeepChangeEvent(PublicTypes.upkeep(upkeep), PublicTypes.cause(cause)));
    }
}
