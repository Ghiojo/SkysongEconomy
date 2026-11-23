package org.skysongdev.skysongEconomy.prices.modifier;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * What is being priced, so modifiers can tell whether they apply.
 *
 * @param direction   an import, an export, or a Claim
 * @param businessId  the business trading, or {@code null}
 * @param ledgerId    the ledger the import or export comes from, or {@code null}
 * @param materialIds the items involved: everything an import can deliver, or the item an export sells
 * @param goodTypes   the good types of those items
 * @param exemptFrom  the target kinds whose modifiers this trade ignores, from a tax-exempt ledger;
 *                    never {@link TargetKind#LEDGER}, because modifiers on the ledger always apply
 * @param claimId     the SkysongClaims Claim being bought, or {@code null} for a trade
 * @param claimTypeId that Claim's type, or {@code null} for a trade
 */
public record PriceSubject(TradeDirection direction, @Nullable String businessId, @Nullable String ledgerId,
                           Set<String> materialIds, Set<String> goodTypes, Set<TargetKind> exemptFrom,
                           @Nullable String claimId, @Nullable String claimTypeId) {
    public PriceSubject {
        Objects.requireNonNull(direction, "direction");
        materialIds = Set.copyOf(materialIds);
        goodTypes = Set.copyOf(goodTypes);
        exemptFrom = Set.copyOf(exemptFrom);
        if (exemptFrom.contains(TargetKind.LEDGER)) {
            throw new IllegalArgumentException("A trade can't be exempt from its own ledger's modifiers");
        }
    }

    /** A trade, which may ignore some modifiers because its ledger is tax exempt. */
    public PriceSubject(TradeDirection direction, @Nullable String businessId, @Nullable String ledgerId,
                        Set<String> materialIds, Set<String> goodTypes, Set<TargetKind> exemptFrom) {
        this(direction, businessId, ledgerId, materialIds, goodTypes, exemptFrom, null, null);
    }

    /** A trade that ignores no modifiers. */
    public PriceSubject(TradeDirection direction, @Nullable String businessId, @Nullable String ledgerId,
                        Set<String> materialIds, Set<String> goodTypes) {
        this(direction, businessId, ledgerId, materialIds, goodTypes, Set.of());
    }

    /**
     * @param claimId     the Claim being bought, in lower case
     * @param claimTypeId its SkysongClaims type
     * @return the subject of buying that Claim, which only {@code on:claims} modifiers on it, its
     *         type or {@code global} change
     */
    public static PriceSubject claim(String claimId, String claimTypeId) {
        return new PriceSubject(TradeDirection.CLAIM, null, null, Set.of(), Set.of(), Set.of(),
                Objects.requireNonNull(claimId, "claimId"), Objects.requireNonNull(claimTypeId, "claimTypeId"));
    }

    /** @return whether a modifier on that target is ignored for this trade, even though it matches */
    public boolean isExemptFrom(ModifierTarget target) {
        return exemptFrom.contains(target.kind());
    }
}
