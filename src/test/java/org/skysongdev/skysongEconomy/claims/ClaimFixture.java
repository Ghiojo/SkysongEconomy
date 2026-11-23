package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.prices.BaseValueRepository;
import org.skysongdev.skysongEconomy.prices.PriceDatabase;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * A small world of Claims for the Claim rules' tests: {@code rose_house} and {@code oak_house}
 * (type {@code house}, not free, unowned), and {@code hovel_1} (a Free Claim). Anna and Bram are
 * two players' characters; Anna's second character is {@link #ANNA_ALT}. The price database
 * knows {@code architite_pound} at 10; prices keep 1 decimal.
 */
final class ClaimFixture {
    static final CharacterRef ANNA = new CharacterRef(UUID.fromString("00000000-0000-0000-0000-00000000000a"), "Anna");
    static final CharacterRef BRAM = new CharacterRef(UUID.fromString("00000000-0000-0000-0000-00000000000b"), "Bram");
    static final CharacterRef ANNA_ALT = new CharacterRef(ANNA.player(), "Annabel");
    static final CharacterRef CARA = new CharacterRef(UUID.fromString("00000000-0000-0000-0000-00000000000c"), "Cara");
    static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    final FakeClaims claims = new FakeClaims().add("rose_house", "house").add("oak_house", "house")
            .addFree("hovel_1", "hovel", 2).setLimit("house", 3);
    final InMemoryClaimPriceRepository storage = new InMemoryClaimPriceRepository();
    final PriceDatabase database = new PriceDatabase(new NoBaseValueStorage());
    final ClaimPrices prices = new ClaimPrices(storage, database, new PriceSettings(1), Clock.fixed(NOW, ZoneOffset.UTC));
    final InMemoryClaimListingRepository listingStorage = new InMemoryClaimListingRepository();
    final ClaimListings listings = new ClaimListings(listingStorage, Clock.fixed(NOW, ZoneOffset.UTC));
    final ClaimSales sales = new ClaimSales(claims, prices, listings);

    ClaimFixture() {
        database.set("architite_pound", "10");
    }

    /** @return the amount with the 1 decimal prices keep, as they are compared */
    static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(1);
    }

    private static final class NoBaseValueStorage implements BaseValueRepository {
        @Override
        public Map<String, String> loadAll() {
            return Map.of();
        }

        @Override
        public void save(String materialId, String expression, BigDecimal valueNow) {
        }

        @Override
        public void delete(String materialId) {
        }
    }
}
