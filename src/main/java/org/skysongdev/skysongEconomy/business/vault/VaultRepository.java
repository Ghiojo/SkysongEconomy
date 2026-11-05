package org.skysongdev.skysongEconomy.business.vault;

import java.math.BigDecimal;
import java.util.Map;

/** Where business vault balances are saved. */
public interface VaultRepository {
    /** @return every saved balance, by business id; a business with none has an empty vault */
    Map<String, BigDecimal> loadAll();

    /** Saves the balance of one business's vault. */
    void save(String businessId, BigDecimal balance);
}
