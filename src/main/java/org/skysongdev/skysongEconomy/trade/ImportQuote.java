package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.prices.modifier.ModifiedPrice;

import java.util.Objects;

/**
 * What an import costs with one delivery time, right now.
 *
 * @param price the bundle's price for that time, with every applying modifier
 */
public record ImportQuote(Ledger ledger, ImportOffer offer, DeliveryTime deliveryTime, ModifiedPrice price) {
    public ImportQuote {
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(offer, "offer");
        Objects.requireNonNull(deliveryTime, "deliveryTime");
        Objects.requireNonNull(price, "price");
    }
}
