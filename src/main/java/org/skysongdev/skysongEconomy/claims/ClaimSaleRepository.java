package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.api.claims.ClaimSale;

/** Where finished Claim sales are recorded: the {@code claim_sales} table. */
public interface ClaimSaleRepository {
    /** @return the number of the last sale recorded, 0 when there is none; read once at start-up */
    long lastId();

    /** Records a finished sale. */
    void save(ClaimSale sale);
}
