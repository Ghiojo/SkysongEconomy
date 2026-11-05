package org.skysongdev.skysongEconomy.trade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceExpression;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierDraft;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.trade.ExportBatch.Stack;
import org.skysongdev.skysongEconomy.trade.ExportBatch.Take;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportServiceTest {
    /** 3 swords, paying {base} * 0.5. */
    private static final ExportOffer SWORDS = new ExportOffer("swords", "Swords", "IRON_SWORD", List.of(),
            new ExportOffer.Target.Item("sword"), 3, PriceExpression.parse("{base} * 0.5"));
    /** Any 4 weapons, paying {base}. */
    private static final ExportOffer WEAPONS = new ExportOffer("weapons", "Weapons", "IRON_SWORD", List.of(),
            new ExportOffer.Target.GoodType("weapon"), 4, PriceExpression.parse("{base}"));
    private static final ExportOffer AXES = new ExportOffer("axes", "Axes", "IRON_AXE", List.of(),
            new ExportOffer.Target.Item("axe"), 3, PriceExpression.parse("{base}"));
    private static final Ledger LEDGER = TradeFixture.SMUGGLERS;

    private final TradeFixture trade = new TradeFixture();
    private final ExportService exports = new ExportService(trade.settings, trade.selections, trade.access, trade.pricing,
            id -> trade.goodTypes.getOrDefault(id, Set.of()), trade.wallet);

    private static String refusal(Executable action) {
        return assertThrows(BusinessException.class, action).messageKey();
    }

    @Test
    void sellsExactlyOneBatchAndPaysTheSeller() {
        List<Stack> inventory = List.of(new Stack(0, "sword", 2), new Stack(1, "spear", 5), new Stack(4, "sword", 2));
        List<Take> removed = new ArrayList<>();
        ExportQuote sold = exports.sell(trade.shop, TradeFixture.OWNER, LEDGER, SWORDS, inventory, removed::addAll);
        assertEquals(List.of(new Take(0, "sword", 2), new Take(4, "sword", 1)), removed);
        // 3 swords * 10 = 30, * 0.5
        assertEquals(0, new BigDecimal("15").compareTo(sold.price().price()));
        assertEquals(0, new BigDecimal("15").compareTo(trade.wallet.balance(TradeFixture.OWNER.player())));
    }

    @Test
    void notAWholeBatchSellsNothing() {
        List<Take> removed = new ArrayList<>();
        assertEquals("business.exports.not-enough", refusal(() -> exports.sell(trade.shop, TradeFixture.OWNER, LEDGER, SWORDS,
                List.of(new Stack(0, "sword", 2)), removed::addAll)));
        assertTrue(removed.isEmpty());
        assertEquals(0, trade.wallet.balance(TradeFixture.OWNER.player()).signum());
    }

    @Test
    void aGoodTypeExportTakesAnyItemOfThatTypeAndPricesWhatWasSold() {
        trade.goodTypes.put("spear", Set.of("weapon"));
        List<Stack> inventory = List.of(new Stack(0, "spear", 3), new Stack(1, "sword", 5));
        ExportQuote quote = exports.quote(trade.shop, LEDGER, WEAPONS, inventory).orElseThrow();
        assertEquals(List.of(new Take(0, "spear", 3), new Take(1, "sword", 1)), quote.takes());
        // 3 * 4 + 1 * 10
        assertEquals(0, new BigDecimal("22").compareTo(quote.price().price()));
        assertEquals(8, exports.count(WEAPONS, inventory));
    }

    @Test
    void workersSellOnlyWhenTheirGroupAllowsIt() {
        List<Stack> inventory = List.of(new Stack(0, "sword", 3));
        assertEquals("business.exports.not-allowed", refusal(() -> exports.sell(trade.shop, TradeFixture.WORKER, LEDGER, SWORDS,
                inventory, takes -> { })));
        trade.access.setPermission(trade.shop, AccessGroup.WORKERS, Permission.SELL_EXPORTS, true);
        exports.sell(trade.shop, TradeFixture.WORKER, LEDGER, SWORDS, inventory, takes -> { });
        assertEquals(0, new BigDecimal("15").compareTo(trade.wallet.balance(TradeFixture.WORKER.player())));
    }

    @Test
    void onlyExportModifiersApply() {
        add("import_tax", AppliesTo.IMPORTS, "50");
        add("export_bonus", AppliesTo.EXPORTS, "10");
        add("everything", AppliesTo.BOTH, "10");
        ExportQuote quote = exports.quote(trade.shop, LEDGER, SWORDS, List.of(new Stack(0, "sword", 3))).orElseThrow();
        assertEquals(2, quote.price().applied().size());
        assertEquals(0, new BigDecimal("18").compareTo(quote.price().price()));
    }

    private void add(String type, AppliesTo appliesTo, String percent) {
        trade.modifiers.add(new ModifierDraft(type, ModifierTarget.GLOBAL, appliesTo, new BigDecimal(percent), null, null, "Staff"));
    }

    @Test
    void theLedgerMustBeSelectedAndItemsPriced() {
        assertThrows(PriceException.class, () -> exports.quote(trade.shop, LEDGER, AXES,
                List.of(new Stack(0, "axe", 3))));
        trade.wallet.balances.put(TradeFixture.OWNER.player(), BigDecimal.TEN);
        trade.selections.remove(trade.shop, LEDGER.id());
        assertEquals("business.ledgers.not-selected", refusal(() -> exports.sell(trade.shop, TradeFixture.OWNER, LEDGER, SWORDS,
                List.of(new Stack(0, "sword", 3)), takes -> { })));
    }

    @Test
    void aFailedPaymentTakesNothing() {
        trade.wallet.refuseDeposits = true;
        List<Take> removed = new ArrayList<>();
        assertEquals("business.exports.pay-failed", refusal(() -> exports.sell(trade.shop, TradeFixture.OWNER, LEDGER, SWORDS,
                List.of(new Stack(0, "sword", 3)), removed::addAll)));
        assertTrue(removed.isEmpty());
    }
}
