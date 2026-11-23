package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierListener;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Keeps the fourth line of Claim signs, which SkysongClaims keeps for SkysongEconomy, showing what
 * a buyer pays: the total with modifiers while SkysongEconomy has the Claim on sale, and nothing
 * otherwise.
 * <p>
 * SkysongClaims saves the line, so it is only written when its text changes; the last text written
 * for each Claim is kept in memory. A Claim it knows nothing about yet (after a start or a reload)
 * is always written. Main thread only.
 */
public final class SignPriceUpdater implements ModifierListener {
    /** What the line says for a quote, as MiniMessage. */
    @FunctionalInterface
    public interface SignText {
        /** @return the line for a Claim on sale at that quote */
        String line(ClaimQuote quote);
    }

    /** Kept for a Claim whose line was cleared. */
    private static final String CLEARED = "";

    private final ClaimGateway claims;
    private final ClaimSales sales;
    private final ClaimPricing pricing;
    private final SignText text;
    private final Consumer<String> warnings;
    /** The last line written for each Claim, {@link #CLEARED} when it was cleared. */
    private final Map<String, String> shown = new HashMap<>();

    /** @param warnings told when a Claim's price can't be worked out, so its line is left blank */
    public SignPriceUpdater(ClaimGateway claims, ClaimSales sales, ClaimPricing pricing, SignText text,
                            Consumer<String> warnings) {
        this.claims = Objects.requireNonNull(claims, "claims");
        this.sales = Objects.requireNonNull(sales, "sales");
        this.pricing = Objects.requireNonNull(pricing, "pricing");
        this.text = Objects.requireNonNull(text, "text");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** Shows the Claim's price now, or clears it when SkysongEconomy isn't selling it. */
    public void refresh(String claimId) {
        claims.find(claimId).ifPresentOrElse(this::refresh, () -> shown.remove(claimId));
    }

    /** Writes the Claim's line even if it hasn't changed, since a sign was just linked to it and starts blank. */
    public void redraw(String claimId) {
        shown.remove(claimId);
        refresh(claimId);
    }

    /** Works out again the line of every Claim on sale, such as after a base value changed. */
    public void refreshListed() {
        sales.listed().forEach(this::refresh);
    }

    /**
     * Writes again every line SkysongEconomy may have written: every Claim with a price or on sale
     * by us. Used at start-up and when SkysongClaims reloads, when the signs' lines are unknown.
     */
    public void redrawAll() {
        shown.clear();
        Set<String> ids = new LinkedHashSet<>(sales.prices().all().keySet());
        sales.listed().forEach(claim -> ids.add(claim.id()));
        ids.forEach(this::refresh);
    }

    /** A Claim was deleted: forgets its line. */
    public void forget(String claimId) {
        shown.remove(claimId);
    }

    /** A modifier on Claims started or stopped: every Claim on sale is worked out again. */
    @Override
    public void modifierChanged(PriceModifier modifier) {
        if (modifier.appliesTo() == AppliesTo.CLAIMS) {
            refreshListed();
        }
    }

    private void refresh(ClaimView claim) {
        String line = sales.isOurs(claim) ? line(claim) : CLEARED;
        if (line.equals(shown.get(claim.id()))) {
            return;
        }
        try {
            if (line.equals(CLEARED)) {
                claims.clearPriceLine(claim.id());
            } else {
                claims.setPriceLine(claim.id(), line);
            }
            shown.put(claim.id(), line);
        } catch (RuntimeException e) {
            // SkysongClaims refused it (the Claim went away meanwhile): try again next time.
            shown.remove(claim.id());
            warnings.accept("The sign price of the Claim " + claim.id() + " could not be written: " + e.getMessage());
        }
    }

    private String line(ClaimView claim) {
        try {
            return text.line(pricing.quote(claim.id()));
        } catch (ClaimException | PriceException e) {
            warnings.accept("The Claim " + claim.id() + " is on sale but its price can't be worked out ("
                    + e.messageKey() + "), so its sign shows none");
            return CLEARED;
        }
    }
}
