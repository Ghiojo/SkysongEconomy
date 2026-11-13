package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents;

/** Fires {@link BusinessNpcCreateEvent} and {@link BusinessNpcRemoveEvent}. */
public final class BukkitBusinessNpcEvents implements BusinessNpcEvents {
    private final PluginManager plugins;

    public BukkitBusinessNpcEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void created(Business business, BusinessNpc npc) {
        plugins.callEvent(new BusinessNpcCreateEvent(business, npc));
    }

    @Override
    public void removed(Business business, BusinessNpc npc, RemovalCause cause) {
        plugins.callEvent(new BusinessNpcRemoveEvent(business, npc, cause));
    }
}
