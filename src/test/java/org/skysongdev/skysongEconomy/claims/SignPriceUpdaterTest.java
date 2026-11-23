package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.NOW;

class SignPriceUpdaterTest {
    private final ClaimFixture fixture = new ClaimFixture();
    private final MovableClock clock = new MovableClock();
    private final PriceModifiers modifiers = new PriceModifiers(new NoModifierStorage(), clock);
    private final ClaimPricing pricing = new ClaimPricing(fixture.claims, fixture.prices, modifiers, new PriceSettings(1), clock);
    private final List<String> warnings = new ArrayList<>();
    /** Shows the total, with "+" when modifiers add to it, so tests can read the line. */
    private final SignPriceUpdater signs = new SignPriceUpdater(fixture.claims, fixture.sales, pricing,
            quote -> quote.total().toPlainString() + (quote.isModified() ? "+" : ""), warnings::add);

    SignPriceUpdaterTest() {
        // Wired as the plugin wires it.
        fixture.sales.addListener(signs::refresh);
        fixture.database.addListener(signs::refreshListed);
        modifiers.addListener(signs);
    }

    private Optional<String> line(String claimId) {
        return fixture.claims.priceLine(claimId);
    }

    private PriceModifier add(String key, ModifierTarget target, String flat, Duration lasts) {
        return modifiers.add(new ModifierDraft(key, target, AppliesTo.CLAIMS, new ModifierAmount.Flat(new BigDecimal(flat)),
                null, lasts == null ? null : NOW.plus(lasts), "Staff"));
    }

    @Test
    void showsThePriceOnlyWhileOnSale() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        assertEquals(Optional.empty(), line("rose_house"));

        fixture.sales.sell("rose_house", null, "Staff");
        assertEquals(Optional.of("2000.0"), line("rose_house"));

        fixture.sales.setPrice("rose_house", "2500", "Staff");
        assertEquals(Optional.of("2500.0"), line("rose_house"));

        fixture.sales.unsell("rose_house");
        assertEquals(Optional.empty(), line("rose_house"));
    }

    @Test
    void clearingThePriceClearsTheLine() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        fixture.sales.clearPrice("rose_house");
        assertEquals(Optional.empty(), line("rose_house"));
    }

    @Test
    void anAbandonedClaimShowsItsPriceAgain() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        fixture.claims.addOwner("rose_house", ClaimFixture.ANNA);
        fixture.claims.removeOwner("rose_house", ClaimFixture.ANNA);

        fixture.sales.abandoned("rose_house");

        assertEquals(Optional.of("2000.0"), line("rose_house"));
    }

    @Test
    void onlyWritesWhenTheTextChanges() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        int writes = fixture.claims.priceLineWrites();

        signs.refresh("rose_house");
        signs.refreshListed();
        fixture.sales.setPrice("rose_house", "2000", "Staff");

        assertEquals(writes, fixture.claims.priceLineWrites());
    }

    @Test
    void aNewSignIsWrittenEvenIfThePriceDidNotChange() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        int writes = fixture.claims.priceLineWrites();

        signs.redraw("rose_house");

        assertEquals(writes + 1, fixture.claims.priceLineWrites());
    }

    @Test
    void modifiersChangeTheShownTotal() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        fixture.sales.sell("oak_house", "1000", "Staff");

        PriceModifier tax = add("tax", new ModifierTarget.ClaimType("house"), "100", null);
        assertEquals(Optional.of("2100.0+"), line("rose_house"));
        assertEquals(Optional.of("1100.0+"), line("oak_house"));

        add("damage", new ModifierTarget.Claim("oak_house"), "50", null);
        assertEquals(Optional.of("1150.0+"), line("oak_house"));

        modifiers.remove(tax.id());
        assertEquals(Optional.of("2000.0"), line("rose_house"));
        assertEquals(Optional.of("1050.0+"), line("oak_house"));
    }

    @Test
    void tradeModifiersDontTouchSigns() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        int writes = fixture.claims.priceLineWrites();

        modifiers.add(new ModifierDraft("port_tax", ModifierTarget.GLOBAL, AppliesTo.BOTH,
                new ModifierAmount.Percent(BigDecimal.TEN), null, null, "Staff"));

        assertEquals(writes, fixture.claims.priceLineWrites());
    }

    @Test
    void anExpiredModifierLeavesTheSignWhenPurged() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        add("festival", new ModifierTarget.ClaimType("house"), "100", Duration.ofHours(1));
        assertEquals(Optional.of("2100.0+"), line("rose_house"));

        clock.now = NOW.plus(Duration.ofHours(2));
        modifiers.purgeExpired();

        assertEquals(Optional.of("2000.0"), line("rose_house"));
    }

    @Test
    void aBaseValueChangeUpdatesPricesThatUseIt() {
        fixture.sales.sell("rose_house", "{item:architite_pound} * 100", "Staff");
        assertEquals(Optional.of("1000.0"), line("rose_house"));

        fixture.database.set("architite_pound", "12");

        assertEquals(Optional.of("1200.0"), line("rose_house"));
    }

    @Test
    void aTypeChangeFollowsTheNewTypesModifiers() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        fixture.claims.addType("manor", 1);
        add("tax", new ModifierTarget.ClaimType("manor"), "500", null);

        fixture.claims.setType("rose_house", "manor");
        signs.refresh("rose_house");

        assertEquals(Optional.of("2500.0+"), line("rose_house"));
    }

    @Test
    void redrawAllRewritesEveryPricedClaim() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        fixture.sales.setPrice("oak_house", "1000", "Staff");
        int writes = fixture.claims.priceLineWrites();

        signs.redrawAll();

        // rose_house written again, oak_house cleared in case a stale price was left on it
        assertEquals(writes + 2, fixture.claims.priceLineWrites());
        assertEquals(Optional.of("2000.0"), line("rose_house"));
        assertEquals(Optional.empty(), line("oak_house"));
        assertEquals(Optional.empty(), line("hovel_1"));
    }

    @Test
    void anotherPluginsSaleIsNotShown() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        fixture.claims.setForSaleBy("rose_house", "OtherPlugin");

        signs.refresh("rose_house");

        assertEquals(Optional.empty(), line("rose_house"));
    }

    @Test
    void aPriceThatCantBeWorkedOutShowsNothingAndWarns() {
        fixture.database.set("steel", "5");
        fixture.sales.sell("rose_house", "{item:steel} * 100", "Staff");
        assertEquals(Optional.of("500.0"), line("rose_house"));

        fixture.database.remove("steel");

        assertEquals(Optional.empty(), line("rose_house"));
        assertTrue(warnings.getFirst().contains("rose_house"));
    }

    @Test
    void aDeletedClaimIsForgotten() {
        fixture.sales.sell("rose_house", "2000", "Staff");
        fixture.claims.delete("rose_house");

        signs.forget("rose_house");
        signs.refresh("rose_house");
        signs.refreshListed();

        assertTrue(warnings.isEmpty());
    }

    private static final class MovableClock extends Clock {
        Instant now = NOW;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
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
