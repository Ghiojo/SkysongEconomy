package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepChangeEvent;
import org.skysongdev.skysongEconomy.api.business.NpcUpkeepPaidEvent;
import org.skysongdev.skysongEconomy.trade.ItemAmount;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents;

import java.time.Instant;
import java.util.List;

/** Fires {@link NpcUpkeepChangeEvent} and {@link NpcUpkeepPaidEvent}. */
public final class BukkitUpkeepEvents implements UpkeepEvents {
    private final PluginManager plugins;

    public BukkitUpkeepEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void stateChanged(NpcUpkeep upkeep, Cause cause) {
        plugins.callEvent(new NpcUpkeepChangeEvent(PublicTypes.upkeep(upkeep), PublicTypes.cause(cause)));
    }

    @Override
    public void cyclePaid(NpcUpkeep upkeep, Instant due, List<ItemAmount> eaten) {
        plugins.callEvent(new NpcUpkeepPaidEvent(PublicTypes.upkeep(upkeep), due, PublicTypes.items(eaten)));
    }
}
