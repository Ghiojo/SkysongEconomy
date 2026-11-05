package org.skysongdev.skysongEconomy.prices.modifier;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * What is being priced, so modifiers can tell whether they apply.
 *
 * @param direction   an import or an export
 * @param businessId  the business trading, or {@code null}
 * @param ledgerId    the ledger the import or export comes from, or {@code null}
 * @param materialIds the items involved: everything an import can deliver, or the item an export sells
 * @param goodTypes   the good types of those items
 */
public record PriceSubject(TradeDirection direction, @Nullable String businessId, @Nullable String ledgerId,
                           Set<String> materialIds, Set<String> goodTypes) {
    public PriceSubject {
        Objects.requireNonNull(direction, "direction");
        materialIds = Set.copyOf(materialIds);
        goodTypes = Set.copyOf(goodTypes);
    }
}
