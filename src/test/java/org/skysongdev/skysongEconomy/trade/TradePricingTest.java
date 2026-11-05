package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifiedPrice;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierDraft;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradePricingTest {
    private final TradeFixture trade = new TradeFixture();

    private ModifiedPrice price() {
        return trade.pricing.quote(trade.shop, TradeFixture.SMUGGLERS, TradeFixture.CRATE, TradeFixture.EXPRESS).price();
    }

    private void modifier(String type, String target, String percent) {
        trade.modifiers.add(new ModifierDraft(type, ModifierTarget.parse(target), AppliesTo.IMPORTS, new BigDecimal(percent),
                null, null, "Staff"));
    }

    @Test
    void baseIsTheBundleValueAndTheExpressionUsesIt() {
        // base = 2 * 10 + 2.0 * 4 = 28; express = 28 * 2
        ModifiedPrice price = price();
        assertEquals(0, new BigDecimal("56").compareTo(price.base()));
        assertEquals(0, new BigDecimal("56").compareTo(price.price()));
    }

    @Test
    void modifiersOnTheBusinessLedgerItemsAndGoodTypesApply() {
        modifier("a", "business:shop", "10");
        modifier("b", "ledger:smugglers", "10");
        modifier("c", "item:spear", "-5");
        modifier("d", "good-type:weapon", "5");
        modifier("e", "ledger:guild", "50");
        ModifiedPrice price = price();
        assertEquals(4, price.applied().size());
        // 56 * 1.20 = 67.2
        assertEquals(new BigDecimal("67.2"), price.price());
    }

    @Test
    void aDeliveryTimeTheImportLeavesOutHasNoPrice() {
        PriceException refusal = assertThrows(PriceException.class, () ->
                trade.pricing.quote(trade.shop, TradeFixture.SMUGGLERS, TradeFixture.CRATE, TradeFixture.SLOW));
        assertEquals("business.imports.time-not-offered", refusal.messageKey());
    }

    @Test
    void anItemWithoutABaseValueCantBePriced() {
        trade.prices.remove("spear");
        assertEquals("prices.unknown-item", assertThrows(PriceException.class, this::price).messageKey());
    }
}
