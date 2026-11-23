package org.skysongdev.skysongEconomy.trade.pier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.modifier.AppliesTo;
import org.skysongdev.skysongEconomy.prices.modifier.InMemoryModifierRepository;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierAmount;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierDraft;
import org.skysongdev.skysongEconomy.prices.modifier.ModifierTarget;
import org.skysongdev.skysongEconomy.prices.modifier.PriceModifiers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PierPriceModifiersTest {
    private static final PierDetails DOCKS = new PierDetails("Docks", "BARREL", List.of());

    private final PierService piers = new PierService(new PierSettings(Map.of("harbour",
            new ConfiguredPier("harbour", DOCKS, Map.of(), BigDecimal.ZERO, Map.of()))), new InMemoryPierRepository(),
            warning -> { });
    private final PriceModifiers modifiers = new PriceModifiers(new InMemoryModifierRepository(),
            Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC));

    @BeforeEach
    void connect() {
        modifiers.loadAll();
        PierPriceModifiers.connect(piers, modifiers);
    }

    private void addOn(String pier) {
        modifiers.add(new ModifierDraft("dues", ModifierTarget.parse("pier:" + pier), AppliesTo.IMPORTS,
                new ModifierAmount.Percent(BigDecimal.TEN), null, null, "Staff"));
    }

    @Test
    void aModifierMustNameAPier() {
        PriceException refused = assertThrows(PriceException.class, () -> addOn("moon"));
        assertEquals("prices.modifiers.unknown-pier", refused.messageKey());
        addOn("harbour");
        assertEquals(1, modifiers.all().size());
    }

    @Test
    void aRemovedPluginPierTakesItsModifiersWithIt() {
        piers.create("SkysongPolitics", "docks", DOCKS);
        addOn("docks");
        addOn("harbour");

        piers.remove("docks");

        assertEquals(List.of("pier:harbour"), modifiers.all().stream().map(modifier -> modifier.target().key()).toList());
    }

    @Test
    void aPierGoneFromTheFileKeepsItsModifiers() {
        addOn("harbour");

        piers.reload(PierSettings.NONE);

        assertEquals(1, modifiers.all().size());
    }
}
