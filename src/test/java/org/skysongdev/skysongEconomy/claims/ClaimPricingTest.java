package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.prices.PriceException;
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
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.NOW;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.money;

class ClaimPricingTest {
    private final ClaimFixture fixture = new ClaimFixture();
    private final PriceModifiers modifiers = new PriceModifiers(new NoModifierStorage(), Clock.fixed(NOW, ZoneOffset.UTC));
    private final ClaimPricing pricing = new ClaimPricing(fixture.claims, fixture.prices, modifiers, new PriceSettings(1),
            Clock.fixed(NOW, ZoneOffset.UTC));

    private PriceModifier add(String key, ModifierTarget target, ModifierAmount amount) {
        return modifiers.add(new ModifierDraft(key, target, AppliesTo.CLAIMS, amount, null, null, "Staff"));
    }

    private static ModifierAmount percent(String value) {
        return new ModifierAmount.Percent(new BigDecimal(value));
    }

    private static ModifierAmount flat(String value) {
        return new ModifierAmount.Flat(new BigDecimal(value));
    }

    @Test
    void withoutModifiersAClaimCostsItsAskingPrice() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(money("2000"), quote.askingPrice());
        assertEquals(money("2000"), quote.total());
        assertEquals(0, quote.modifierTotal().signum());
        assertFalse(quote.isModified());
        assertEquals("house", quote.claimTypeId());
    }

    @Test
    void everyShareComesFromTheAskingPrice() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        PriceModifier tax = add("district_tax", new ModifierTarget.ClaimType("house"), percent("10"));
        PriceModifier fee = add("registry_fee", new ModifierTarget.ClaimType("house"), flat("50"));
        PriceModifier levy = add("levy", ModifierTarget.GLOBAL, percent("5"));

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(List.of(new ClaimQuote.Share(tax, money("200")), new ClaimQuote.Share(fee, money("50")),
                new ClaimQuote.Share(levy, money("100"))), quote.shares());
        assertEquals(money("350"), quote.modifierTotal());
        assertEquals(money("2350"), quote.total());
        assertEquals(money("2000"), quote.askingPrice(), "the seller's part never changes");
    }

    @Test
    void sharesAreTruncatedSoTheyAddUpToTheTotal() {
        fixture.sales.setPrice("rose_house", "999.9", "Staff");
        add("tax", new ModifierTarget.ClaimType("house"), percent("3.33"));
        add("fee", new ModifierTarget.ClaimType("house"), flat("0.99"));

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(money("33.2"), quote.shares().get(0).amount());
        assertEquals(money("0.9"), quote.shares().get(1).amount());
        assertEquals(quote.askingPrice().add(quote.modifierTotal()), quote.total());
    }

    @Test
    void aDiscountTakesItsShareOffTheAskingPrice() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        PriceModifier relief = add("council_relief", new ModifierTarget.ClaimType("house"), percent("-10"));

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(List.of(new ClaimQuote.Share(relief, money("-200"))), quote.shares());
        assertEquals(money("-200"), quote.modifierTotal());
        assertEquals(money("1800"), quote.total());
        assertEquals(money("2000"), quote.askingPrice(), "the seller's part never changes");
        assertEquals(ClaimQuote.Change.LOWERED, quote.change());
        assertEquals("-", quote.shares().get(0).sign());
        assertEquals(money("200"), quote.shares().get(0).magnitude());
    }

    @Test
    void raisesAndDiscountsAddUp() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        add("council_tax", new ModifierTarget.ClaimType("house"), percent("10"));
        add("festival", ModifierTarget.GLOBAL, percent("-10"));

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(0, quote.modifierTotal().signum());
        assertEquals(money("2000"), quote.total());
        assertTrue(quote.isModified());
        assertEquals(ClaimQuote.Change.NONE, quote.change());
    }

    @Test
    void discountsNeverTakeTheTotalBelowZero() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        add("relief", new ModifierTarget.ClaimType("house"), percent("-80"));
        add("festival", ModifierTarget.GLOBAL, percent("-50"));

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(money("-2000"), quote.modifierTotal());
        assertEquals(0, quote.total().signum());
    }

    @Test
    void aRaisedQuoteSaysSo() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        add("council_tax", new ModifierTarget.ClaimType("house"), percent("10"));

        ClaimQuote quote = pricing.quote("rose_house");

        assertEquals(ClaimQuote.Change.RAISED, quote.change());
        assertEquals("+", quote.shares().get(0).sign());
    }

    @Test
    void typeModifierApiTakesADiscount() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");

        pricing.setTypeModifier("council_relief", "house", percent("-10"), null, null, "SkysongPolitics");

        assertEquals(money("1800"), pricing.quote("rose_house").total());
    }

    @Test
    void modifiersOnlyReachTheirClaims() {
        fixture.claims.addType("manor", 1).add("old_manor", "manor");
        fixture.sales.setPrice("old_manor", "5000", "Staff");
        fixture.sales.setPrice("rose_house", "2000", "Staff");
        add("tax", new ModifierTarget.ClaimType("house"), percent("10"));
        add("damage", new ModifierTarget.Claim("oak_house"), flat("30"));
        modifiers.add(new ModifierDraft("port_tax", ModifierTarget.GLOBAL, AppliesTo.BOTH, percent("50"), null, null, "Staff"));

        assertEquals(money("5000"), pricing.quote("old_manor").total());
        assertEquals(money("2200"), pricing.quote("rose_house").total());
    }

    @Test
    void typeModifierApi() {
        fixture.sales.setPrice("rose_house", "2000", "Staff");

        PriceModifiers.SetResult set = pricing.setTypeModifier("district_tax", "House", percent("10"), Duration.ofDays(7),
                "Harbour district tax", "TreasuryPlugin");
        PriceModifiers.SetResult replaced = pricing.setTypeModifier("district_tax", "house", percent("20"), null, null,
                "TreasuryPlugin");

        assertTrue(replaced.replaced().isPresent());
        assertEquals(set.modifier().id(), replaced.modifier().id());
        assertEquals(List.of(replaced.modifier()), pricing.modifiersOf("house"));
        assertEquals(money("2400"), pricing.quote("rose_house").total());
        assertEquals(NOW.plus(Duration.ofDays(7)), set.modifier().expiresAt());

        assertEquals(replaced.modifier(), pricing.removeTypeModifier("district_tax", "house").orElseThrow());
        assertTrue(pricing.removeTypeModifier("district_tax", "house").isEmpty());
        assertEquals(money("2000"), pricing.quote("rose_house").total());
        assertThrows(PriceException.class, () -> pricing.setTypeModifier("festival", "house", flat("-10"), null, null, "X"),
                "a flat discount is refused");
    }

    @Test
    void checksClaimTargetsAgainstSkysongClaims() {
        modifiers.addTargetCheck(pricing::checkTarget);

        assertEquals("prices.modifiers.unknown-claim-type", assertThrows(PriceException.class,
                () -> add("tax", new ModifierTarget.ClaimType("hous"), percent("10"))).messageKey());
        assertEquals("prices.modifiers.unknown-claim", assertThrows(PriceException.class,
                () -> add("tax", new ModifierTarget.Claim("nowhere"), percent("10"))).messageKey());
        add("tax", new ModifierTarget.ClaimType("house"), percent("10"));
        add("damage", new ModifierTarget.Claim("rose_house"), flat("10"));
    }

    @Test
    void aDeletedClaimLosesItsModifiers() {
        add("damage", new ModifierTarget.Claim("rose_house"), flat("10"));
        add("tax", new ModifierTarget.ClaimType("house"), percent("10"));

        pricing.deleted("rose_house");

        assertEquals(1, modifiers.all().size());
    }

    @Test
    void refusesWhatCantBeQuoted() {
        assertEquals("claims.unknown-claim", assertThrows(ClaimException.class, () -> pricing.quote("nowhere")).messageKey());
        assertEquals("claims.free", assertThrows(ClaimException.class, () -> pricing.quote("hovel_1")).messageKey());
        assertEquals("claims.no-price", assertThrows(ClaimException.class, () -> pricing.quote("rose_house")).messageKey());
    }

    @Test
    void withoutBusinessesThereAreNoModifiers() {
        ClaimPricing plain = new ClaimPricing(fixture.claims, fixture.prices, null, new PriceSettings(1),
                Clock.fixed(NOW, ZoneOffset.UTC));
        fixture.sales.setPrice("rose_house", "2000", "Staff");

        assertEquals(money("2000"), plain.quote("rose_house").total());
        assertEquals(List.of(), plain.modifiersOf("house"));
        assertEquals("claims.modifiers-off", assertThrows(ClaimException.class,
                () -> plain.setTypeModifier("tax", "house", percent("10"), null, null, "X")).messageKey());
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
