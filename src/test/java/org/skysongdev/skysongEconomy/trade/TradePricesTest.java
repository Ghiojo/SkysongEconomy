package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.trade.AppliedTradeModifier;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifiedPrice;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierAmount;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradePricesTest {
    private static PriceModifier modifier(String id, ModifierTarget target, String percent) {
        return new PriceModifier(id, id + "_type", target, AppliesTo.BOTH,
                new ModifierAmount.Percent(new BigDecimal(percent)), "Name " + id, null, "CONSOLE", Instant.EPOCH);
    }

    @Test
    void copiesTheBaseThePaidPriceAndEachModifiersShare() {
        ModifiedPrice price = new ModifiedPrice(new BigDecimal("200"),
                List.of(modifier("m1", new ModifierTarget.Global(), "10"),
                        modifier("m2", new ModifierTarget.Ledger("smugglers"), "-2.5")),
                List.of(), new BigDecimal("7.5"), new BigDecimal("215"));

        TradePrice trade = TradePrices.of(price);

        assertEquals(0, new BigDecimal("200").compareTo(trade.base()));
        assertEquals(0, new BigDecimal("7.5").compareTo(trade.percent()));
        assertEquals(0, new BigDecimal("215").compareTo(trade.paid()));
        assertFalse(trade.taxExempt());
        AppliedTradeModifier tax = trade.modifiers().get(0);
        assertEquals("m1", tax.id());
        assertEquals("m1_type", tax.type());
        assertEquals("global", tax.target());
        assertEquals("Name m1", tax.displayName());
        assertEquals(0, new BigDecimal("20").compareTo(tax.share()));
        AppliedTradeModifier discount = trade.modifiers().get(1);
        assertEquals(new ModifierTarget.Ledger("smugglers").key(), discount.target());
        assertEquals(0, new BigDecimal("-5").compareTo(discount.share()));
    }

    @Test
    void isTaxExemptWhenTheLedgerSkippedModifiers() {
        ModifiedPrice price = new ModifiedPrice(BigDecimal.TEN, List.of(),
                List.of(modifier("m1", new ModifierTarget.Global(), "10")), BigDecimal.ZERO, BigDecimal.TEN);

        TradePrice trade = TradePrices.of(price);

        assertTrue(trade.taxExempt());
        assertEquals(List.of(), trade.modifiers());
    }

    @Test
    void aFlatPriceHasNoModifiers() {
        TradePrice flat = TradePrice.flat(BigDecimal.ONE);

        assertEquals(BigDecimal.ONE, flat.base());
        assertEquals(BigDecimal.ONE, flat.paid());
        assertEquals(0, BigDecimal.ZERO.compareTo(flat.percent()));
        assertEquals(List.of(), flat.modifiers());
    }
}
