package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.skysongdev.skysongEconomy.api.claims.AppliedModifier;
import org.skysongdev.skysongEconomy.api.claims.ClaimSale;
import org.skysongdev.skysongEconomy.api.claims.Seller;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.MapWallet;
import org.skysongdev.skysongEconomy.prices.PriceSettings;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierAmount;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierDraft;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierRepository;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifier;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.ANNA;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.ANNA_ALT;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.BRAM;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.CARA;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.NOW;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.money;

class ClaimPurchasesTest {
    /** A fourth player's character. */
    private static final CharacterRef DARA = new CharacterRef(UUID.fromString("00000000-0000-0000-0000-00000000000d"), "Dara");

    private final ClaimFixture fixture = new ClaimFixture();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PriceModifiers modifiers = new PriceModifiers(new NoModifierStorage(), clock);
    private final ClaimPricing pricing = new ClaimPricing(fixture.claims, fixture.prices, modifiers, new PriceSettings(1), clock);
    private final MapWallet wallet = new MapWallet();
    private final InMemoryClaimSaleRepository records = new InMemoryClaimSaleRepository();
    private final InMemoryClaimPayoutRepository payoutStorage = new InMemoryClaimPayoutRepository();
    private final ClaimPayouts payouts = new ClaimPayouts(payoutStorage, wallet);
    private final List<ClaimSale> events = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final ClaimPurchases purchases = new ClaimPurchases(fixture.claims, fixture.sales, pricing, wallet, payouts,
            new PriceSettings(1), records, events::add, warnings::add, clock);

    ClaimPurchasesTest() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        wallet.balances.put(ANNA.player(), money("5000"));
    }

    private static String refusal(Executable action) {
        return assertThrows(ClaimException.class, action).messageKey();
    }

    private PriceModifier add(String key, ModifierTarget target, ModifierAmount amount) {
        return modifiers.add(new ModifierDraft(key, target, AppliesTo.CLAIMS, amount, "District tax", null, "Staff"));
    }

    @Test
    void buyingAServerSaleMakesTheBuyerTheOwnerAndPaysNobody() {
        ClaimSale sale = purchases.buy(ANNA, "rose_house", money("2000"));

        assertEquals(List.of(ANNA), fixture.claims.find("rose_house").orElseThrow().owners());
        assertFalse(fixture.claims.find("rose_house").orElseThrow().isForSale());
        assertEquals(money("3000"), wallet.balance(ANNA.player()));
        assertEquals(1, wallet.balances.size(), "nobody else was paid");
        assertEquals(new ClaimSale(1, "rose_house", "house", ANNA, Optional.empty(), money("2000"), List.of(),
                BigDecimal.ZERO, money("2000"), false, NOW), sale);
        assertEquals(List.of(sale), records.saved);
        assertEquals(List.of(sale), events);
    }

    @Test
    void theSaleReportsEveryModifiersShare() {
        PriceModifier tax = add("district_tax", new ModifierTarget.ClaimType("house"), new ModifierAmount.Percent(BigDecimal.TEN));
        PriceModifier fee = add("registry_fee", new ModifierTarget.Claim("rose_house"), new ModifierAmount.Flat(new BigDecimal("50")));

        ClaimSale sale = purchases.buy(ANNA, "rose_house", money("2250"));

        assertEquals(List.of(
                new AppliedModifier(tax.id(), "district_tax", "claim-type:house", "percent", BigDecimal.TEN, money("200"),
                        "District tax"),
                new AppliedModifier(fee.id(), "registry_fee", "claim:rose_house", "flat", new BigDecimal("50"), money("50"),
                        "District tax")), sale.modifiers());
        assertEquals(money("250"), sale.modifierTotal());
        assertEquals(money("2000"), sale.askingPrice());
        assertEquals(money("2250"), sale.paid());
        assertEquals(money("2750"), wallet.balance(ANNA.player()));
    }

    @Test
    void salesCountOnFromTheLastRecorded() {
        records.lastId = 41;
        purchases.load();
        fixture.sales.sell("oak_house", "100", "Staff");

        assertEquals(42, purchases.buy(ANNA, "rose_house", money("2000")).id());
        assertEquals(43, purchases.buy(ANNA, "oak_house", money("100")).id());
    }

    @Test
    void aChangedPriceIsNotBought() {
        assertEquals("claims.buy.price-changed", refusal(() -> purchases.buy(ANNA, "rose_house", money("1900"))));
        assertNothingHappened();
    }

    @Test
    void notEnoughMoneyBuysNothing() {
        wallet.balances.put(ANNA.player(), money("1999"));
        assertEquals("claims.buy.no-money", refusal(() -> purchases.buy(ANNA, "rose_house", money("2000"))));
        assertEquals(money("1999"), wallet.balance(ANNA.player()));
        assertNothingHappened();
    }

    @Test
    void onlyClaimsOnSaleBySkysongEconomyCanBeBought() {
        assertEquals("claims.buy.not-for-sale", refusal(() -> purchases.offer(ANNA, "oak_house")));
        fixture.claims.setForSaleBy("oak_house", "OtherPlugin");
        assertEquals("claims.buy.not-for-sale", refusal(() -> purchases.offer(ANNA, "oak_house")));
        assertEquals("claims.unknown-claim", refusal(() -> purchases.offer(ANNA, "nowhere")));
    }

    @Test
    void skysongClaimsLimitsAreChecked() {
        fixture.claims.setLimit("house", 0);
        assertEquals("claims.buy.type-limit", refusal(() -> purchases.buy(ANNA, "rose_house", money("2000"))));
        fixture.claims.setLimit("house", 3).forget(ANNA);
        assertEquals("claims.buy.unknown-character", refusal(() -> purchases.offer(ANNA, "rose_house")));
        assertNothingHappened();
    }

    @Test
    void aRefusalAfterPayingGivesTheMoneyBack() {
        fixture.claims.refuseNextOwner = OwnershipAnswer.TYPE_LIMIT;

        assertEquals("claims.buy.type-limit", refusal(() -> purchases.buy(ANNA, "rose_house", money("2000"))));

        assertEquals(money("5000"), wallet.balance(ANNA.player()));
        assertNothingHappened();
    }

    @Test
    void aFailedRefundIsReportedForStaff() {
        fixture.claims.refuseNextOwner = OwnershipAnswer.FULL;
        wallet.refuseDeposits = true;

        refusal(() -> purchases.buy(ANNA, "rose_house", money("2000")));

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("rose_house"));
    }

    @Test
    void theBoughtClaimShowsNoPriceAndKeepsItsOwnForLater() {
        SignPriceUpdater signs = new SignPriceUpdater(fixture.claims, fixture.sales, pricing, quote -> "price", warnings::add);
        fixture.sales.addListener(signs::refresh);
        signs.redrawAll();
        assertEquals(Optional.of("price"), fixture.claims.priceLine("rose_house"));

        purchases.buy(ANNA, "rose_house", money("2000"));

        assertEquals(Optional.empty(), fixture.claims.priceLine("rose_house"));
        assertEquals(money("2000"), fixture.prices.value("rose_house").orElseThrow());
    }

    @Test
    void anotherCharacterCanOnlyBuyWhatIsStillOnSale() {
        wallet.balances.put(BRAM.player(), money("5000"));
        ClaimQuote seen = purchases.offer(BRAM, "rose_house");
        purchases.buy(ANNA, "rose_house", money("2000"));

        assertEquals("claims.buy.not-for-sale", refusal(() -> purchases.buy(BRAM, "rose_house", seen.total())));
        assertEquals(money("5000"), wallet.balance(BRAM.player()));
    }

    @Test
    void aResaleSplitsTheAskingPriceAndKeepsTheModifiersOut() {
        fixture.claims.addOwner("oak_house", BRAM);
        fixture.claims.addOwner("oak_house", CARA);
        fixture.sales.listByOwner(BRAM, "oak_house", "1001");
        add("district_tax", new ModifierTarget.ClaimType("house"), new ModifierAmount.Percent(BigDecimal.TEN));

        ClaimSale sale = purchases.buy(ANNA, "oak_house", money("1101.1"));

        assertEquals(List.of(ANNA), fixture.claims.find("oak_house").orElseThrow().owners());
        // 1001 / 2 = 500.5 each; the 100.1 of modifiers goes to nobody
        assertEquals(money("500.5"), wallet.balance(BRAM.player()));
        assertEquals(money("500.5"), wallet.balance(CARA.player()));
        assertEquals(money("3898.9"), wallet.balance(ANNA.player()));
        Seller seller = sale.seller().orElseThrow();
        assertEquals(BRAM, seller.listedBy());
        assertEquals(List.of(BRAM, CARA), seller.owners());
        assertEquals(Map.of(BRAM.player(), money("500.5"), CARA.player(), money("500.5")), seller.payouts());
        assertFalse(sale.sellerPaidLater());
        assertTrue(fixture.listings.find("oak_house").isEmpty());
    }

    @Test
    void theListerGetsWhatTruncationLeavesOver() {
        fixture.claims.addOwner("oak_house", BRAM);
        fixture.claims.addOwner("oak_house", CARA);
        fixture.claims.setLimit("house", 5);
        fixture.claims.addOwner("oak_house", ANNA_ALT);
        fixture.sales.listByOwner(CARA, "oak_house", "100");
        wallet.balances.put(DARA.player(), money("1000"));

        ClaimSale sale = purchases.buy(DARA, "oak_house", money("100"));

        // 100 / 3 = 33.3 each, 0.1 left over for Cara
        assertEquals(Map.of(BRAM.player(), money("33.3"), CARA.player(), money("33.4"), ANNA.player(), money("33.3")),
                sale.seller().orElseThrow().payouts());
    }

    @Test
    void aShareThatCantBePaidWaitsForItsSeller() {
        fixture.claims.addOwner("oak_house", BRAM);
        fixture.sales.listByOwner(BRAM, "oak_house", "1000");
        wallet.refuseDeposits = true;

        ClaimSale sale = purchases.buy(ANNA, "oak_house", money("1000"));

        assertTrue(sale.sellerPaidLater());
        assertEquals(List.of(new PendingPayout(sale.id(), BRAM.player(), "oak_house", money("1000"), NOW)),
                payouts.owed(BRAM.player()));
        assertEquals(payouts.owed(BRAM.player()), payoutStorage.saved);
    }

    @Test
    void aRefusedResaleGivesTheClaimBackToItsOwners() {
        fixture.claims.addOwner("oak_house", BRAM);
        fixture.claims.addOwner("oak_house", CARA);
        fixture.sales.listByOwner(BRAM, "oak_house", "1000");
        fixture.claims.refuseNextOwner = OwnershipAnswer.TYPE_LIMIT;

        refusal(() -> purchases.buy(ANNA, "oak_house", money("1000")));

        assertEquals(List.of(BRAM, CARA), fixture.claims.find("oak_house").orElseThrow().owners());
        assertTrue(fixture.claims.find("oak_house").orElseThrow().isForSale(), "still on sale, not relisted by the server");
        assertTrue(fixture.listings.find("oak_house").isPresent());
        assertEquals(money("5000"), wallet.balance(ANNA.player()));
        assertEquals(BigDecimal.ZERO, wallet.balance(BRAM.player()));
    }

    @Test
    void noOneBuysAClaimOneOfTheirCharactersOwns() {
        fixture.claims.addOwner("oak_house", ANNA_ALT);
        fixture.sales.listByOwner(ANNA_ALT, "oak_house", "1000");

        assertEquals("claims.buy.own-claim", refusal(() -> purchases.offer(ANNA, "oak_house")));
    }

    @Test
    void theOldOwnersLeavingDoesNotPutItBackOnSale() {
        fixture.claims.addOwner("oak_house", BRAM);
        fixture.sales.listByOwner(BRAM, "oak_house", "1000");
        // As SkysongClaims does: an owner removed fires ClaimOwnerRemoveEvent, and the listener calls abandoned.
        fixture.claims.onOwnersCleared = fixture.sales::abandoned;

        purchases.buy(ANNA, "oak_house", money("1000"));

        assertFalse(fixture.claims.find("oak_house").orElseThrow().isForSale());
    }

    private void assertNothingHappened() {
        assertTrue(fixture.claims.find("rose_house").orElseThrow().owners().isEmpty());
        assertTrue(fixture.claims.find("rose_house").orElseThrow().isForSale());
        assertTrue(records.saved.isEmpty());
        assertTrue(events.isEmpty());
    }

    private static final class NoModifierStorage implements ModifierRepository {
        @Override
        public List<PriceModifier> loadAll() {
            return List.of();
        }

        @Override
        public void save(PriceModifier modifier) {
        }

        @Override
        public void delete(String id) {
        }
    }
}
