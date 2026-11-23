package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.prices.PriceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.NOW;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.money;

class ClaimPricesTest {
    private final ClaimFixture fixture = new ClaimFixture();
    private final ClaimPrices prices = fixture.prices;

    @Test
    void setsAndSavesAPrice() {
        assertEquals(money("2000"), prices.set("Rose_House", "2000", "Staff"));

        ClaimPrice price = prices.find("rose_house").orElseThrow();
        assertEquals("rose_house", price.claimId());
        assertEquals("Staff", price.setBy());
        assertEquals(NOW, price.setAt());
        assertEquals(new ClaimPriceRepository.Row("rose_house", "2000", "Staff", NOW.toEpochMilli()),
                fixture.storage.rows.get("rose_house"));
    }

    @Test
    void followsTheItemsItNamesAndTruncates() {
        prices.set("rose_house", "{item:architite_pound} * 600 + 0.19", "Staff");
        assertEquals(Optional.of(money("6000.1")), prices.value("rose_house"));

        fixture.database.set("architite_pound", "12");
        assertEquals(Optional.of(money("7200.1")), prices.value("rose_house"));
    }

    @Test
    void refusesBadPrices() {
        PriceException base = assertThrows(PriceException.class, () -> prices.set("rose_house", "{base} * 2", "Staff"));
        assertEquals("prices.no-base", base.messageKey());
        PriceException unknown = assertThrows(PriceException.class,
                () -> prices.set("rose_house", "{item:moon_rock}", "Staff"));
        assertEquals("prices.unknown-item", unknown.messageKey());
        assertThrows(PriceException.class, () -> prices.set("rose_house", "20 *", "Staff"));
        ClaimException zero = assertThrows(ClaimException.class, () -> prices.set("rose_house", "0.04", "Staff"));
        assertEquals("claims.price-not-positive", zero.messageKey());
        assertThrows(ClaimException.class, () -> prices.set("rose_house", "-5", "Staff"));

        assertTrue(prices.find("rose_house").isEmpty());
        assertTrue(fixture.storage.rows.isEmpty());
    }

    @Test
    void removes() {
        prices.set("rose_house", "2000", "Staff");

        assertTrue(prices.remove("ROSE_HOUSE"));
        assertFalse(prices.remove("rose_house"));
        assertTrue(prices.find("rose_house").isEmpty());
        assertTrue(fixture.storage.rows.isEmpty());
    }

    @Test
    void loadsSavedPricesAndSkipsBrokenOnes() {
        fixture.storage.rows.put("rose_house", new ClaimPriceRepository.Row("rose_house", "2500", "Staff", 5));
        fixture.storage.rows.put("oak_house", new ClaimPriceRepository.Row("oak_house", "(oops", "Staff", 5));
        List<String> warnings = new ArrayList<>();

        assertEquals(1, prices.loadAll(warnings::add));

        assertEquals(Optional.of(money("2500")), prices.value("rose_house"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("oak_house"));
        assertTrue(fixture.storage.rows.containsKey("oak_house"), "a broken row stays saved for staff to fix");
    }
}
