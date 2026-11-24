package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.api.claims.ClaimSaleChange;

/**
 * Told about every change to a Claim's price or sale made through {@link ClaimSales}, after it is
 * made, so other plugins can hear about it. Called on the main thread.
 */
public interface ClaimSaleEvents {
    /** Tells nobody. */
    ClaimSaleEvents NONE = change -> {
    };

    void changed(ClaimSaleChange change);
}
