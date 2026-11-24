package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.trade.LedgerSelectionChange;
import org.skysongdev.skysongEconomy.api.trade.LedgerSelectionChangeEvent;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessDirectory;
import org.skysongdev.skysongEconomy.trade.LedgerSelectionEvents;

/** Fires {@link LedgerSelectionChangeEvent}. */
public final class BukkitLedgerSelectionEvents implements LedgerSelectionEvents {
    private final PluginManager plugins;

    public BukkitLedgerSelectionEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void changed(Business business, LedgerSelectionChange change) {
        plugins.callEvent(new LedgerSelectionChangeEvent(BusinessDirectory.info(business), change));
    }
}
