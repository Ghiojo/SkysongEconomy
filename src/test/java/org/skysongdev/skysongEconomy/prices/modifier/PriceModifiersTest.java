package org.skysongdev.skysongEconomy.prices.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.prices.PriceModifierChange;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceModifiersTest {
    private static final Instant START = Instant.parse("2026-11-05T12:00:00Z");
    private static final PriceSettings ONE_DECIMAL = new PriceSettings(1);
    private static final PriceSubject WINE_IMPORT = new PriceSubject(TradeDirection.IMPORT, "tavern", "smugglers",
            Set.of("red_wine"), Set.of("alcohol", "wine"));

    private final MutableClock clock = new MutableClock(START);
    private final InMemoryModifierRepository repository = new InMemoryModifierRepository();
    private PriceModifiers modifiers;

    @BeforeEach
    void setUp() {
        modifiers = new PriceModifiers(repository, clock);
        modifiers.loadAll();
    }

    private static ModifierAmount percent(String value) {
        return new ModifierAmount.Percent(new BigDecimal(value));
    }

    private static ModifierDraft draft(String type, String target, String percent) {
        return new ModifierDraft(type, ModifierTarget.parse(target), AppliesTo.IMPORTS, percent(percent), null, null, "Staff");
    }

    @Test
    void differentTypesAddUp() {
        modifiers.add(draft("alcohol_tax", "good-type:alcohol", "10"));
        modifiers.add(draft("wine_glut", "good-type:wine", "-5"));
        ModifiedPrice price = modifiers.price(new BigDecimal("100"), WINE_IMPORT, ONE_DECIMAL);
        assertEquals(0, new BigDecimal("5").compareTo(price.percent()));
        assertEquals(0, new BigDecimal("105").compareTo(price.price()));
        assertEquals(2, price.applied().size());
    }

    @Test
    void theSameTypeOnTheSameTargetIsRefusedByAddAndReplacedBySet() {
        PriceModifier first = modifiers.add(draft("port_tax", "ledger:smugglers", "10"));
        PriceException refusal = assertThrows(PriceException.class,
                () -> modifiers.add(draft("port_tax", "ledger:smugglers", "20")));
        assertEquals("prices.modifiers.exists", refusal.messageKey());

        PriceModifiers.SetResult result = modifiers.set(draft("port_tax", "ledger:smugglers", "20"));
        assertEquals(first.id(), result.modifier().id());
        assertEquals(first, result.replaced().orElseThrow());
        assertEquals(1, modifiers.all().size());
        assertEquals(0, new BigDecimal("20").compareTo(repository.saved.get(first.id()).amount().value()));
    }

    @Test
    void aPierModifierMatchesImportsSentToThatPier() {
        modifiers.add(draft("harbour_dues", "PIER:Harbour", "10"));

        assertEquals(0, new BigDecimal("100").compareTo(modifiers.price(new BigDecimal("100"), WINE_IMPORT, ONE_DECIMAL).price()));
        assertEquals(0, new BigDecimal("110").compareTo(modifiers.price(new BigDecimal("100"), WINE_IMPORT.toPier("harbour"), ONE_DECIMAL).price()));
        assertEquals(0, new BigDecimal("100").compareTo(modifiers.price(new BigDecimal("100"), WINE_IMPORT.toPier("cove"), ONE_DECIMAL).price()));
        assertEquals(TargetKind.PIER, ModifierTarget.parse("pier:harbour").kind());
    }

    @Test
    void aPierModifierCantApplyOnlyToExportsOrClaims() {
        PriceException exports = assertThrows(PriceException.class, () -> modifiers.add(new ModifierDraft("dues",
                ModifierTarget.parse("pier:harbour"), AppliesTo.EXPORTS, percent("10"), null, null, "Staff")));
        assertEquals("prices.modifiers.pier-imports-only", exports.messageKey());
        assertThrows(PriceException.class, () -> modifiers.add(new ModifierDraft("dues",
                ModifierTarget.parse("pier:harbour"), AppliesTo.CLAIMS, percent("10"), null, null, "Staff")));
        modifiers.add(new ModifierDraft("dues", ModifierTarget.parse("pier:harbour"), AppliesTo.BOTH, percent("10"),
                null, null, "Staff"));
    }

    @Test
    void theSameTypeOnAnotherTargetStacks() {
        modifiers.add(draft("port_tax", "ledger:smugglers", "10"));
        modifiers.add(draft("port_tax", "global", "10"));
        assertEquals(0, new BigDecimal("120").compareTo(modifiers.price(new BigDecimal("100"), WINE_IMPORT, ONE_DECIMAL).price()));
    }

    @Test
    void eachTargetKindMatchesItsSubjects() {
        PriceSubject other = new PriceSubject(TradeDirection.IMPORT, "smithy", "guild", Set.of("steel_ingot"), Set.of("metal"));
        for (String target : List.of("global", "business:tavern", "ledger:smugglers", "good-type:wine", "item:red_wine")) {
            assertTrue(ModifierTarget.parse(target).matches(WINE_IMPORT), target);
            assertEquals(target.equals("global"), ModifierTarget.parse(target).matches(other), target);
        }
    }

    @Test
    void exportsAreOnlyChangedWhenAskedFor() {
        PriceSubject export = new PriceSubject(TradeDirection.EXPORT, "tavern", "smugglers", Set.of("red_wine"), Set.of());
        modifiers.add(draft("port_tax", "global", "10"));
        assertEquals(List.of(), modifiers.applying(export));
        modifiers.add(new ModifierDraft("export_bonus", ModifierTarget.GLOBAL, AppliesTo.BOTH, percent("10"), null, null, "Staff"));
        assertEquals(1, modifiers.applying(export).size());
        assertEquals(2, modifiers.applying(WINE_IMPORT).size());
    }

    @Test
    void anExemptTradeSkipsGeneralModifiersButPaysItsLedgersOwn() {
        PriceSubject exempt = new PriceSubject(TradeDirection.IMPORT, "tavern", "smugglers", Set.of("red_wine"),
                Set.of("alcohol", "wine"), Set.of(TargetKind.GLOBAL, TargetKind.GOOD_TYPE, TargetKind.ITEM));
        modifiers.add(draft("crown_tax", "global", "10"));
        modifiers.add(draft("embargo", "good-type:wine", "50"));
        modifiers.add(draft("duty", "item:red_wine", "5"));
        modifiers.add(draft("fine", "business:tavern", "20"));
        modifiers.add(draft("bribe", "ledger:smugglers", "10"));

        ModifiedPrice price = modifiers.price(new BigDecimal("100"), exempt, ONE_DECIMAL);
        assertEquals(List.of("fine", "bribe"), price.applied().stream().map(PriceModifier::type).toList());
        assertEquals(List.of("crown_tax", "embargo", "duty"), price.skipped().stream().map(PriceModifier::type).toList());
        assertEquals(0, new BigDecimal("130").compareTo(price.price()));
    }

    @Test
    void anExemptTradeSkipsGeneralDiscountsToo() {
        PriceSubject exempt = new PriceSubject(TradeDirection.IMPORT, "tavern", "smugglers", Set.of("red_wine"),
                Set.of(), Set.of(TargetKind.GLOBAL));
        modifiers.add(draft("festival", "global", "-15"));
        ModifiedPrice price = modifiers.price(new BigDecimal("100"), exempt, ONE_DECIMAL);
        assertEquals(0, new BigDecimal("100").compareTo(price.price()));
        assertEquals(1, price.skipped().size());
    }

    @Test
    void aNormalTradeSkipsNothing() {
        modifiers.add(draft("crown_tax", "global", "10"));
        ModifiedPrice price = modifiers.price(new BigDecimal("100"), WINE_IMPORT, ONE_DECIMAL);
        assertEquals(List.of(), price.skipped());
        assertEquals(0, new BigDecimal("110").compareTo(price.price()));
    }

    @Test
    void aTradeCantBeExemptFromItsOwnLedger() {
        assertThrows(IllegalArgumentException.class, () -> new PriceSubject(TradeDirection.IMPORT, "tavern", "smugglers",
                Set.of(), Set.of(), Set.of(TargetKind.LEDGER)));
    }

    @Test
    void pricesAreTruncatedAndNeverNegative() {
        modifiers.add(draft("a", "global", "3"));
        assertEquals(new BigDecimal("4.2"), modifiers.price(new BigDecimal("4.09"), WINE_IMPORT, ONE_DECIMAL).price());
        modifiers.add(draft("b", "global", "-100"));
        modifiers.add(draft("c", "global", "-50"));
        assertEquals(0, modifiers.price(new BigDecimal("10"), WINE_IMPORT, ONE_DECIMAL).price().signum());
    }

    @Test
    void expiredModifiersStopApplyingAndAreDeleted() {
        modifiers.add(new ModifierDraft("festival", ModifierTarget.GLOBAL, AppliesTo.IMPORTS, percent("-10"), null,
                START.plus(Duration.ofHours(1)), "Staff"));
        assertEquals(1, modifiers.applying(WINE_IMPORT).size());
        clock.now = START.plus(Duration.ofHours(1));
        assertEquals(List.of(), modifiers.applying(WINE_IMPORT));
        assertEquals(List.of(), modifiers.all());
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void refusesInvalidValues() {
        assertKey("prices.modifiers.invalid-type", draft("Port Tax", "global", "10"));
        assertKey("prices.modifiers.invalid-percent", draft("tax", "global", "0"));
        assertKey("prices.modifiers.invalid-percent", draft("tax", "global", "-101"));
        assertKey("prices.modifiers.already-expired", new ModifierDraft("tax", ModifierTarget.GLOBAL, AppliesTo.IMPORTS,
                percent("10"), null, START, "Staff"));
        assertKey("prices.modifiers.invalid-name", new ModifierDraft("tax", ModifierTarget.GLOBAL, AppliesTo.IMPORTS,
                percent("10"), "x".repeat(PriceModifiers.MAX_DISPLAY_NAME_LENGTH + 1), null, "Staff"));
    }

    @Test
    void claimModifiersOnlyRaiseClaimPrices() {
        assertKey("prices.modifiers.flat-needs-claims", new ModifierDraft("fee", ModifierTarget.GLOBAL, AppliesTo.IMPORTS,
                flat("10"), null, null, "Staff"));
        assertKey("prices.modifiers.claim-discount", new ModifierDraft("festival", ModifierTarget.GLOBAL, AppliesTo.CLAIMS,
                percent("-15"), null, null, "Staff"));
        assertKey("prices.modifiers.claim-discount", new ModifierDraft("damage", new ModifierTarget.Claim("rose_house"),
                AppliesTo.CLAIMS, flat("-30"), null, null, "Staff"));
        assertKey("prices.modifiers.not-a-claim-target", new ModifierDraft("tax", ModifierTarget.parse("ledger:smugglers"),
                AppliesTo.CLAIMS, percent("10"), null, null, "Staff"));
        assertKey("prices.modifiers.claims-only-target", new ModifierDraft("tax", new ModifierTarget.ClaimType("house"),
                AppliesTo.BOTH, percent("10"), null, null, "Staff"));
        assertKey("prices.modifiers.invalid-flat", new ModifierDraft("fee", ModifierTarget.GLOBAL, AppliesTo.CLAIMS,
                flat("1000000"), null, null, "Staff"));

        PriceModifier fee = modifiers.add(new ModifierDraft("registry_fee", new ModifierTarget.ClaimType("house"),
                AppliesTo.CLAIMS, flat("50"), null, null, "Staff"));
        assertEquals("registry_fee +50", fee.displayName());
        assertEquals(List.of(fee), modifiers.applying(PriceSubject.claim("rose_house", "house")));
        assertEquals(List.of(), modifiers.applying(PriceSubject.claim("big_manor", "manor")));
        assertEquals(List.of(), modifiers.applying(WINE_IMPORT));
    }

    @Test
    void tradeModifiersNeverReachClaims() {
        modifiers.add(new ModifierDraft("festival", ModifierTarget.GLOBAL, AppliesTo.BOTH, percent("-15"), null, null, "Staff"));
        PriceModifier claims = modifiers.add(new ModifierDraft("levy", ModifierTarget.GLOBAL, AppliesTo.CLAIMS, percent("5"),
                null, null, "Staff"));

        assertEquals(List.of(claims), modifiers.applying(PriceSubject.claim("rose_house", "house")));
        assertEquals(1, modifiers.applying(WINE_IMPORT).size());
    }

    @Test
    void targetChecksRefuseUnknownTargets() {
        modifiers.addTargetCheck(target -> {
            if (target instanceof ModifierTarget.ClaimType type && !type.typeId().equals("house")) {
                throw new PriceException("prices.modifiers.unknown-claim-type", Map.of("type", type.typeId()));
            }
        });

        assertKey("prices.modifiers.unknown-claim-type", new ModifierDraft("tax", new ModifierTarget.ClaimType("hous"),
                AppliesTo.CLAIMS, percent("10"), null, null, "Staff"));
        modifiers.add(new ModifierDraft("tax", new ModifierTarget.ClaimType("house"), AppliesTo.CLAIMS, percent("10"),
                null, null, "Staff"));
    }

    @Test
    void listenersHearEveryChange() {
        List<String> heard = new ArrayList<>();
        modifiers.addListener(modifier -> heard.add(modifier.type() + " " + modifier.amount().signed()));

        PriceModifier tax = modifiers.add(draft("tax", "global", "10"));
        modifiers.set(draft("tax", "global", "20"));
        modifiers.remove(tax.id());
        modifiers.add(new ModifierDraft("festival", ModifierTarget.GLOBAL, AppliesTo.IMPORTS, percent("-10"), null,
                START.plus(Duration.ofHours(1)), "Staff"));
        clock.now = START.plus(Duration.ofHours(1));
        modifiers.purgeExpired();

        assertEquals(List.of("tax +10%", "tax +20%", "tax +20%", "festival -10%", "festival -10%"), heard);
    }

    @Test
    void listenersHearWhatKindOfChangeItWas() {
        List<String> heard = new ArrayList<>();
        modifiers.addListener(new ModifierListener() {
            @Override
            public void modifierChanged(PriceModifier modifier) {
            }

            @Override
            public void modifierChanged(PriceModifierChange.Kind kind, PriceModifier modifier,
                                        Optional<PriceModifier> previous) {
                heard.add(kind + " " + modifier.amount().signed()
                        + previous.map(old -> " was " + old.amount().signed()).orElse(""));
            }
        });

        PriceModifier tax = modifiers.add(draft("tax", "global", "10"));
        modifiers.set(draft("tax", "global", "20"));
        modifiers.set(draft("rent", "business:tavern", "5"));
        modifiers.remove(tax.id());
        modifiers.removeAll(new ModifierTarget.Business("tavern"));
        modifiers.add(new ModifierDraft("festival", ModifierTarget.GLOBAL, AppliesTo.IMPORTS, percent("-10"), null,
                START.plus(Duration.ofHours(1)), "Staff"));
        clock.now = START.plus(Duration.ofHours(1));
        modifiers.purgeExpired();

        assertEquals(List.of("ADDED +10%", "REPLACED +20% was +10%", "ADDED +5%", "REMOVED +20%", "REMOVED +5%",
                "ADDED -10%", "EXPIRED -10%"), heard);
    }

    private static ModifierAmount flat(String value) {
        return new ModifierAmount.Flat(new BigDecimal(value));
    }

    private void assertKey(String key, ModifierDraft draft) {
        assertEquals(key, assertThrows(PriceException.class, () -> modifiers.add(draft)).messageKey());
    }

    @Test
    void removeAndRemoveAll() {
        PriceModifier tax = modifiers.add(draft("tax", "business:tavern", "10"));
        modifiers.add(draft("rent", "business:tavern", "5"));
        modifiers.add(draft("tax", "global", "5"));
        assertEquals(tax, modifiers.remove(tax.id()));
        assertEquals("prices.modifiers.not-found",
                assertThrows(PriceException.class, () -> modifiers.remove(tax.id())).messageKey());
        assertEquals(1, modifiers.removeAll(new ModifierTarget.Business("tavern")));
        assertEquals(1, modifiers.all().size());
        assertEquals(1, repository.saved.size());
    }

    @Test
    void namesDefaultToTypeAndPercent() {
        assertEquals("port_tax +10%", modifiers.add(draft("port_tax", "global", "10")).displayName());
        assertEquals("glut -5.5%", modifiers.add(draft("glut", "global", "-5.50")).displayName());
    }

    @Test
    void idsKeepCountingAfterARestart() {
        modifiers.add(draft("a", "global", "1"));
        modifiers.add(draft("b", "global", "1"));
        PriceModifiers restarted = new PriceModifiers(repository, clock);
        assertEquals(2, restarted.loadAll());
        assertEquals("m3", restarted.add(draft("c", "global", "1")).id());
        assertFalse(restarted.find("m1").isEmpty());
    }

    private static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
