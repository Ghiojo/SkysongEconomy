package org.skysongdev.skysongEconomy.integration.skysongclaims;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongClaims.api.event.ClaimDeleteEvent;
import org.skysongdev.skysongClaims.api.event.ClaimOwnerRemoveEvent;
import org.skysongdev.skysongClaims.api.event.ClaimSaleChangeEvent;
import org.skysongdev.skysongClaims.api.event.ClaimSignChangeEvent;
import org.skysongdev.skysongClaims.api.event.ClaimTypeChangeEvent;
import org.skysongdev.skysongClaims.api.event.SkysongClaimsReloadEvent;
import org.skysongdev.skysongEconomy.claims.ClaimPricing;
import org.skysongdev.skysongEconomy.claims.ClaimSales;
import org.skysongdev.skysongEconomy.claims.SignPriceUpdater;

import java.util.logging.Logger;

/**
 * Follows SkysongClaims' changes that matter to sales: a Claim whose last owner left goes back on
 * sale at its price, a deleted Claim's price and the modifiers on it are removed, and sign line 4
 * is written again when a sign is linked, a Claim changes type or sale, or SkysongClaims reloads.
 */
public final class ClaimChangeListener implements Listener {
    private final ClaimSales sales;
    private final ClaimPricing pricing;
    private final SignPriceUpdater signs;
    private final Logger logger;

    public ClaimChangeListener(ClaimSales sales, ClaimPricing pricing, SignPriceUpdater signs, Logger logger) {
        this.sales = sales;
        this.pricing = pricing;
        this.signs = signs;
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
        pricing.deleted(event.getClaim().id());
        signs.forget(event.getClaim().id());
    }

    /** A new sign starts with a blank line 4, so it gets the price at once. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSignChange(ClaimSignChangeEvent event) {
        if (event.isLinked()) {
            signs.redraw(event.getSign().claimId());
        }
    }

    /** Claim type modifiers follow the new type. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTypeChange(ClaimTypeChangeEvent event) {
        signs.refresh(event.getClaim().id());
    }

    /** Covers sales changed outside SkysongEconomy, such as staff taking a Claim off sale in SkysongClaims. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSaleChange(ClaimSaleChangeEvent event) {
        signs.refresh(event.getClaim().id());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReload(SkysongClaimsReloadEvent event) {
        signs.redrawAll();
    }
}
