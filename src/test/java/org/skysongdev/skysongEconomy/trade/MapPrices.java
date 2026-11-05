package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.PriceContext;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.math.BigDecimal;
import java.util.Map;

/** Base values from a map, for tests. */
record MapPrices(Map<String, BigDecimal> values) implements PriceContext {
    static MapPrices of(Object... idsAndValues) {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        for (int i = 0; i < idsAndValues.length; i += 2) {
            values.put((String) idsAndValues[i], new BigDecimal(idsAndValues[i + 1].toString()));
        }
        return new MapPrices(values);
    }

    @Override
    public BigDecimal base() {
        throw new PriceException("prices.no-base", Map.of());
    }

    @Override
    public BigDecimal item(String materialId) {
        BigDecimal value = values.get(materialId);
        if (value == null) {
            throw new PriceException("prices.unknown-item", Map.of("item", materialId));
        }
        return value;
    }
}
