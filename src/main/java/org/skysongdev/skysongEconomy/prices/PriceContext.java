package org.skysongdev.skysongEconomy.prices;

import java.math.BigDecimal;

/** Where an expression gets the values of {@code {base}} and {@code {item:<material_id>}}. */
public interface PriceContext {
    /**
     * @return the value of {@code {base}}: the base value of what is being priced
     * @throws PriceException {@code prices.no-base} when the expression is used where
     *                        {@code {base}} has no meaning, such as the price database
     */
    BigDecimal base();

    /**
     * @return the base value of that item in the price database
     * @throws PriceException {@code prices.unknown-item} when it has none
     */
    BigDecimal item(String materialId);
}
