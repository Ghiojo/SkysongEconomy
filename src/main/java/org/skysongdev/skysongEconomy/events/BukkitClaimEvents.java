package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.claims.ClaimPayoutEvent;
import org.skysongdev.skysongEconomy.api.claims.ClaimSaleChange;
import org.skysongdev.skysongEconomy.api.claims.ClaimSaleChangeEvent;
import org.skysongdev.skysongEconomy.claims.ClaimPayoutEvents;
import org.skysongdev.skysongEconomy.claims.ClaimSaleEvents;
import org.skysongdev.skysongEconomy.claims.PendingPayout;

/** Fires {@link ClaimSaleChangeEvent} and {@link ClaimPayoutEvent}. */
public final class BukkitClaimEvents implements ClaimSaleEvents, ClaimPayoutEvents {
    private final PluginManager plugins;

    public BukkitClaimEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void changed(ClaimSaleChange change) {
        plugins.callEvent(new ClaimSaleChangeEvent(change));
    }

    @Override
    public void paidOut(PendingPayout payout) {
        plugins.callEvent(new ClaimPayoutEvent(payout.account(), payout.claimId(), payout.saleId(), payout.amount(),
                payout.soldAt()));
    }
}
