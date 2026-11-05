package org.skysongdev.skysongEconomy.prices;

import java.math.BigDecimal;
import java.util.Map;

/** Where the price database is saved. */
public interface BaseValueRepository {
    /** @return every item's base value expression, by material id */
    Map<String, String> loadAll();

    /**
     * Saves an item's expression.
     *
     * @param valueNow what it is worth right now, kept for tools that read the table directly
     */
    void save(String materialId, String expression, BigDecimal valueNow);

    void delete(String materialId);
}
