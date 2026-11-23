package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.ANNA;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.BRAM;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.NOW;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.money;

class ClaimSalesTest {
    private final ClaimFixture fixture = new ClaimFixture();
    private final FakeClaims claims = fixture.claims;
    private final ClaimSales sales = fixture.sales;

    private ClaimView claim(String id) {
        return claims.find(id).orElseThrow();
    }

    private static String refusal(Runnable action) {
        return assertThrows(ClaimException.class, action::run).messageKey();
    }

    @Test
    void sellsAtAGivenPriceAndSavesIt() {
        assertEquals(money("2000"), sales.sell("rose_house", "2000", "Staff"));

        assertTrue(claim("rose_house").isForSaleBy(ClaimSales.SELLER));
        assertEquals(money("2000"), fixture.prices.value("rose_house").orElseThrow());
        assertEquals(List.of(claim("rose_house")), sales.listed());
    }

    @Test
    void sellsAtTheSavedPrice() {
        sales.setPrice("rose_house", "1500", "Staff");

        assertEquals(money("1500"), sales.sell("rose_house", null, "Staff"));
        assertTrue(claim("rose_house").isForSale());
    }

    @Test
    void sellingAgainOnlyChangesThePrice() {
        sales.sell("rose_house", "2000", "Staff");

        assertEquals(money("2500"), sales.sell("rose_house", "2500", "Staff"));
        assertTrue(claim("rose_house").isForSaleBy(ClaimSales.SELLER));
    }

    @Test
    void refusesWhatTheServerCantSell() {
        assertEquals("claims.unknown-claim", refusal(() -> sales.sell("nowhere", "10", "Staff")));
        assertEquals("claims.free", refusal(() -> sales.sell("hovel_1", "10", "Staff")));
        assertEquals("claims.free", refusal(() -> sales.setPrice("hovel_1", "10", "Staff")));
        assertEquals("claims.no-price", refusal(() -> sales.sell("rose_house", null, "Staff")));

        claims.addOwner("oak_house", ANNA);
        assertEquals("claims.owned", refusal(() -> sales.sell("oak_house", "10", "Staff")));

        claims.setForSaleBy("rose_house", "OtherPlugin");
        ClaimException elsewhere = assertThrows(ClaimException.class, () -> sales.sell("rose_house", "10", "Staff"));
        assertEquals("claims.sold-elsewhere", elsewhere.messageKey());
        assertEquals("OtherPlugin", elsewhere.placeholders().get("plugin"));

        assertTrue(fixture.prices.all().isEmpty(), "a refused sale saves no price");
    }

    @Test
    void unsellKeepsThePrice() {
        sales.sell("rose_house", "2000", "Staff");

        sales.unsell("rose_house");

        assertFalse(claim("rose_house").isForSale());
        assertTrue(fixture.prices.find("rose_house").isPresent());
        assertEquals("claims.not-for-sale", refusal(() -> sales.unsell("rose_house")));
        claims.setForSaleBy("oak_house", "OtherPlugin");
        assertEquals("claims.not-for-sale", refusal(() -> sales.unsell("oak_house")));
    }

    @Test
    void clearingThePriceTakesItOffSale() {
        sales.sell("rose_house", "2000", "Staff");
        sales.setPrice("oak_house", "900", "Staff");

        assertTrue(sales.clearPrice("rose_house"));
        assertFalse(sales.clearPrice("oak_house"));

        assertFalse(claim("rose_house").isForSale());
        assertTrue(fixture.prices.all().isEmpty());
        assertEquals("claims.no-price", refusal(() -> sales.clearPrice("rose_house")));
    }

    @Test
    void anAbandonedPricedClaimGoesBackOnSale() {
        sales.setPrice("rose_house", "2000", "Staff");
        claims.addOwner("rose_house", ANNA);
        claims.addOwner("oak_house", ANNA);

        claims.removeOwner("rose_house", ANNA);
        claims.removeOwner("oak_house", ANNA);

        assertTrue(sales.abandoned("rose_house"));
        assertTrue(claim("rose_house").isForSaleBy(ClaimSales.SELLER));
        assertFalse(sales.abandoned("oak_house"), "no price, so nothing to sell it for");
        assertFalse(claim("oak_house").isForSale());
    }

    @Test
    void abandonedIgnoresOwnedFreeAndListedClaims() {
        sales.setPrice("rose_house", "2000", "Staff");
        claims.addOwner("rose_house", ANNA);
        assertFalse(sales.abandoned("rose_house"));

        assertFalse(sales.abandoned("hovel_1"));
        assertFalse(sales.abandoned("nowhere"));

        sales.setPrice("oak_house", "900", "Staff");
        claims.setForSaleBy("oak_house", "OtherPlugin");
        assertFalse(sales.abandoned("oak_house"));
        assertTrue(claim("oak_house").isForSaleBy("OtherPlugin"));
    }

    @Test
    void aDeletedClaimLosesItsPrice() {
        sales.setPrice("rose_house", "2000", "Staff");
        claims.delete("rose_house");

        sales.deleted("rose_house");

        assertTrue(fixture.prices.all().isEmpty());
    }

    @Test
    void reconcileFixesWhatChangedWhileOff() {
        sales.setPrice("rose_house", "2000", "Staff");
        sales.sell("oak_house", "900", "Staff");
        claims.addType("manor", 1).add("old_manor", "manor");
        sales.setPrice("old_manor", "5000", "Staff");
        claims.delete("rose_house");
        fixture.prices.remove("oak_house");
        List<String> changes = new ArrayList<>();

        sales.reconcile(changes::add);

        assertEquals(List.of("old_manor"), List.copyOf(fixture.prices.all().keySet()));
        assertFalse(claim("oak_house").isForSale());
        assertEquals(2, changes.size());
    }

    @Test
    void anOwnerListsTheWholeClaimAndTheirPriceBecomesItsPrice() {
        claims.addOwner("rose_house", ANNA);
        claims.addOwner("rose_house", BRAM);

        assertEquals(money("3000"), sales.listByOwner(BRAM, "rose_house", "3000"));

        assertTrue(sales.isOurs(claim("rose_house")));
        assertEquals(Optional.of(new ClaimListing("rose_house", BRAM, NOW)), fixture.listings.find("rose_house"));
        assertEquals(fixture.listings.find("rose_house").orElseThrow(), fixture.listingStorage.saved.get("rose_house"));
        assertEquals(money("3000"), fixture.prices.value("rose_house").orElseThrow());
        assertEquals("Bram", fixture.prices.find("rose_house").orElseThrow().setBy());
    }

    @Test
    void onlyOwnersListAndNeverFreeClaims() {
        claims.addOwner("rose_house", ANNA);
        assertEquals("claims.sell.not-owner", refusal(() -> sales.listByOwner(BRAM, "rose_house", "3000")));
        claims.addOwner("hovel_1", ANNA);
        assertEquals("claims.free", refusal(() -> sales.listByOwner(ANNA, "hovel_1", "3000")));
        claims.setForSaleBy("rose_house", "OtherPlugin");
        assertEquals("claims.sold-elsewhere", refusal(() -> sales.listByOwner(ANNA, "rose_house", "3000")));
        claims.addOwner("oak_house", ANNA);
        assertEquals("claims.price-not-positive", refusal(() -> sales.listByOwner(ANNA, "oak_house", "0")));
    }

    @Test
    void anyOwnerCanTakeItOffSale() {
        claims.addOwner("rose_house", ANNA);
        claims.addOwner("rose_house", BRAM);
        sales.listByOwner(ANNA, "rose_house", "3000");

        assertEquals("claims.sell.not-owner", refusal(() -> sales.unsellByOwner(ClaimFixture.CARA, "rose_house")));
        sales.unsellByOwner(BRAM, "rose_house");

        assertFalse(claim("rose_house").isForSale());
        assertTrue(fixture.listings.find("rose_house").isEmpty());
        assertTrue(fixture.listingStorage.saved.isEmpty());
        assertEquals(money("3000"), fixture.prices.value("rose_house").orElseThrow());
    }

    @Test
    void aListingWhoseOwnersAllLeftBecomesAServerSale() {
        claims.addOwner("rose_house", ANNA);
        sales.listByOwner(ANNA, "rose_house", "3000");

        claims.removeOwner("rose_house", ANNA);
        assertFalse(sales.abandoned("rose_house"));

        assertTrue(sales.isOurs(claim("rose_house")));
        assertTrue(fixture.listings.find("rose_house").isEmpty());
    }

    @Test
    void ownersLeavingDuringATransferIsNotAnAbandonment() {
        sales.setPrice("rose_house", "2000", "Staff");
        claims.addOwner("rose_house", ANNA);

        boolean relisted = sales.transfer("rose_house", () -> {
            claims.clearOwners("rose_house");
            return sales.abandoned("rose_house");
        });

        assertFalse(relisted);
        assertFalse(claim("rose_house").isForSale());
    }

    @Test
    void reconcileDropsListingsThatNoLongerHold() {
        claims.addOwner("rose_house", ANNA);
        claims.addOwner("oak_house", BRAM);
        sales.listByOwner(ANNA, "rose_house", "3000");
        sales.listByOwner(BRAM, "oak_house", "1000");
        claims.removeOwner("rose_house", ANNA);
        claims.setForSaleBy("oak_house", "OtherPlugin");
        List<String> changes = new ArrayList<>();

        sales.reconcile(changes::add);

        assertTrue(fixture.listings.all().isEmpty());
        assertTrue(sales.isOurs(claim("rose_house")), "it stays on sale, for the server");
        assertEquals(2, changes.size());
    }

    @Test
    void soldAndDeletedClaimsLoseTheirListing() {
        claims.addOwner("rose_house", ANNA);
        claims.addOwner("oak_house", ANNA);
        sales.listByOwner(ANNA, "rose_house", "3000");
        sales.listByOwner(ANNA, "oak_house", "1000");

        sales.sold("rose_house");
        sales.deleted("oak_house");

        assertTrue(fixture.listings.all().isEmpty());
        assertFalse(claim("rose_house").isForSale());
    }
}
