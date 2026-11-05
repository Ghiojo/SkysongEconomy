package org.skysongdev.skysongEconomy.prices.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceSettings;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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

    private static ModifierDraft draft(String type, String target, String percent) {
        return new ModifierDraft(type, ModifierTarget.parse(target), AppliesTo.IMPORTS, new BigDecimal(percent), null, null, "Staff");
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
        assertEquals(0, new BigDecimal("20").compareTo(repository.saved.get(first.id()).percent()));
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
        modifiers.add(new ModifierDraft("export_bonus", ModifierTarget.GLOBAL, AppliesTo.BOTH, BigDecimal.TEN, null, null, "Staff"));
        assertEquals(1, modifiers.applying(export).size());
        assertEquals(2, modifiers.applying(WINE_IMPORT).size());
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
        modifiers.add(new ModifierDraft("festival", ModifierTarget.GLOBAL, AppliesTo.IMPORTS, BigDecimal.valueOf(-10), null,
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
                BigDecimal.TEN, null, START, "Staff"));
        assertKey("prices.modifiers.invalid-name", new ModifierDraft("tax", ModifierTarget.GLOBAL, AppliesTo.IMPORTS,
                BigDecimal.TEN, "x".repeat(PriceModifiers.MAX_DISPLAY_NAME_LENGTH + 1), null, "Staff"));
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
