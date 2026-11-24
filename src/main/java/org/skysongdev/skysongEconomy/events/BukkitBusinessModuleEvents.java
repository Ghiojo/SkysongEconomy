package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.business.BusinessModulePickEvent;
import org.skysongdev.skysongEconomy.api.business.BusinessModuleRemoveEvent;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessDirectory;
import org.skysongdev.skysongEconomy.business.module.BusinessModuleEvents;
import org.skysongdev.skysongEconomy.business.module.ModulePick;

/** Fires {@link BusinessModulePickEvent} and {@link BusinessModuleRemoveEvent}. */
public final class BukkitBusinessModuleEvents implements BusinessModuleEvents {
    private final PluginManager plugins;

    public BukkitBusinessModuleEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void picked(Business business, ModulePick pick) {
        plugins.callEvent(new BusinessModulePickEvent(BusinessDirectory.info(business), PublicTypes.pick(pick)));
    }

    @Override
    public void removed(Business business, ModulePick pick, RemovalCause cause) {
        plugins.callEvent(new BusinessModuleRemoveEvent(BusinessDirectory.info(business), PublicTypes.pick(pick), PublicTypes.cause(cause)));
    }
}
