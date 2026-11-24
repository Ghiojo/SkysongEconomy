package org.skysongdev.skysongEconomy.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.trade.ExportSale;
import org.skysongdev.skysongEconomy.api.trade.ExportSellEvent;
import org.skysongdev.skysongEconomy.api.trade.ExportSoldEvent;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessDirectory;
import org.skysongdev.skysongEconomy.trade.ExportEvents;

/** Fires {@link ExportSellEvent} and {@link ExportSoldEvent}. */
public final class BukkitExportEvents implements ExportEvents {
    private final PluginManager plugins;

    public BukkitExportEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public Verdict selling(Business business, ExportSale sale) {
        Player player = Bukkit.getPlayer(sale.seller().player());
        if (player == null) {
            // Exports are sold from a menu, so the seller is online; refuse rather than fire without one.
            return Verdict.cancel(null);
        }
        ExportSellEvent event = new ExportSellEvent(BusinessDirectory.info(business), sale, player);
        plugins.callEvent(event);
        return event.isCancelled() ? Verdict.cancel(event.getCancelMessage().orElse(null)) : Verdict.ALLOW;
    }

    @Override
    public void sold(Business business, ExportSale sale) {
        plugins.callEvent(new ExportSoldEvent(BusinessDirectory.info(business), sale,
                Bukkit.getPlayer(sale.seller().player())));
    }
}
