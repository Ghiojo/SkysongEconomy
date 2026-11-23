package org.skysongdev.skysongEconomy.integration.skysongclaims;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongClaims.api.event.ClaimDeleteEvent;
import org.skysongdev.skysongClaims.api.event.ClaimOwnerRemoveEvent;
import org.skysongdev.skysongEconomy.claims.ClaimSales;

import java.util.logging.Logger;

/**
 * Follows SkysongClaims' changes that matter to sales: a Claim whose last owner left goes back on
 * sale at its price, and a deleted Claim's price is removed.
 */
public final class ClaimChangeListener implements Listener {
    private final ClaimSales sales;
    private final Logger logger;

    public ClaimChangeListener(ClaimSales sales, Logger logger) {
        this.sales = sales;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOwnerRemove(ClaimOwnerRemoveEvent event) {
        if (!event.getClaim().isOwned() && sales.abandoned(event.getClaim().id())) {
            logger.info("The Claim " + event.getClaim().id() + " has no owners left and is for sale again");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDelete(ClaimDeleteEvent event) {
        sales.deleted(event.getClaim().id());
    }
}
