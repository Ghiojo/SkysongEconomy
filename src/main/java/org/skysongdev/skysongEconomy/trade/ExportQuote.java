package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.modifier.ModifiedPrice;

import java.util.List;
import java.util.Objects;

/**
 * What selling one batch pays, right now.
 *
 * @param takes the items the batch takes
 * @param price what the seller gets, with every applying export modifier
 */
public record ExportQuote(Ledger ledger, ExportOffer offer, List<ExportBatch.Take> takes, ModifiedPrice price) {
    public ExportQuote {
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(offer, "offer");
        takes = List.copyOf(takes);
        Objects.requireNonNull(price, "price");
    }
}
