package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.api.trade.AppliedTradeModifier;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.prices.modifier.ModifiedPrice;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierAmount;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.math.BigDecimal;

/** Turns the price rules' {@link ModifiedPrice} into the {@link TradePrice} other plugins see. */
public final class TradePrices {
    private TradePrices() {
    }

    /** @return the public copy of a trade's price */
    public static TradePrice of(ModifiedPrice price) {
        return new TradePrice(price.base(), price.percent(), price.price(),
                price.applied().stream().map(modifier -> applied(price.base(), modifier)).toList(),
                !price.skipped().isEmpty());
    }

    private static AppliedTradeModifier applied(BigDecimal base, PriceModifier modifier) {
        BigDecimal percent = modifier.amount() instanceof ModifierAmount.Percent ? modifier.amount().value() : BigDecimal.ZERO;
        return new AppliedTradeModifier(modifier.id(), modifier.type(), modifier.target().key(), percent,
                base.multiply(percent).movePointLeft(2), modifier.displayName());
    }
}
