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
 * @param exemptFrom  the target kinds whose modifiers this trade ignores, from a tax-exempt ledger;
 *                    never {@link TargetKind#LEDGER}, because modifiers on the ledger always apply
 */
public record PriceSubject(TradeDirection direction, @Nullable String businessId, @Nullable String ledgerId,
                           Set<String> materialIds, Set<String> goodTypes, Set<TargetKind> exemptFrom) {
    public PriceSubject {
        Objects.requireNonNull(direction, "direction");
        materialIds = Set.copyOf(materialIds);
        goodTypes = Set.copyOf(goodTypes);
        exemptFrom = Set.copyOf(exemptFrom);
        if (exemptFrom.contains(TargetKind.LEDGER)) {
            throw new IllegalArgumentException("A trade can't be exempt from its own ledger's modifiers");
        }
    }

    /** A trade that ignores no modifiers. */
    public PriceSubject(TradeDirection direction, @Nullable String businessId, @Nullable String ledgerId,
                        Set<String> materialIds, Set<String> goodTypes) {
        this(direction, businessId, ledgerId, materialIds, goodTypes, Set.of());
    }

    /** @return whether a modifier on that target is ignored for this trade, even though it matches */
    public boolean isExemptFrom(ModifierTarget target) {
        return exemptFrom.contains(target.kind());
    }
}
