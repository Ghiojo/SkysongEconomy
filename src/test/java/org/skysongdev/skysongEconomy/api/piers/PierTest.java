package org.skysongdev.skysongEconomy.api.piers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PierTest {
    private static Pier pier(String base, LossModifier... modifiers) {
        return new Pier("harbour", new PierDetails("Harbour", "oak_boat", List.of()), true, Map.of(),
                new BigDecimal(base), List.of(modifiers), Pier.CONFIG);
    }

    private static LossModifier modifier(String id, String amount, String tag) {
        return LossModifier.parse(id, amount, Optional.ofNullable(tag));
    }

    @Test
    void addsPointsThenAppliesPercentages() {
        Pier pier = pier("2", modifier("guards", "+20", null), modifier("storm", "+50%", null));

        assertEquals(new BigDecimal("33"), pier.finalLossChance(Set.of()));
    }

    @Test
    void sumsPercentagesLikePriceModifiers() {
        Pier pier = pier("10", modifier("storm", "+50%", null), modifier("calm", "-20%", null));

        assertEquals(new BigDecimal("13"), pier.finalLossChance(Set.of()));
    }

    @Test
    void taggedModifiersOnlyCountForThatCargo() {
        Pier pier = pier("1", modifier("guards", "+5", "Contraband"));

        assertEquals(new BigDecimal("1"), pier.finalLossChance(Set.of()));
        assertEquals(new BigDecimal("6"), pier.finalLossChance(Set.of("contraband", "weapons")));
    }

    @Test
    void staysWithinZeroAndHundred() {
        assertEquals(BigDecimal.ZERO, pier("3", modifier("bribes", "-10", null)).finalLossChance(Set.of()));
        assertEquals(new BigDecimal("1E+2"), pier("90", modifier("storm", "+50%", null)).finalLossChance(Set.of()));
        assertEquals(BigDecimal.ZERO, pier("50", modifier("calm", "-150%", null)).finalLossChance(Set.of()));
    }

    @Test
    void readsAndWritesAmounts() {
        LossModifier points = modifier("guards", "5", null);
        assertEquals(new BigDecimal("5"), points.amount());
        assertFalse(points.percent());
        assertEquals("+5", points.signed());

        LossModifier percent = modifier("storm", "-12.5%", "");
        assertTrue(percent.percent());
        assertEquals("-12.5%", percent.signed());
        assertTrue(percent.tag().isEmpty(), "a blank tag means every shipment");

        assertThrows(NumberFormatException.class, () -> modifier("storm", "lots", null));
    }

    @Test
    void checksIds() {
        assertEquals("harbour", Piers.checkId(" Harbour ", "pier id"));
        PierException refused = assertThrows(PierException.class, () -> Piers.checkId("old pier", "pier id"));
        assertEquals("piers.invalid-id", refused.messageKey());
        assertThrows(PierException.class, () -> Piers.checkId("", "pier id"));
        assertThrows(PierException.class, () -> Piers.checkId("x".repeat(65), "pier id"));
    }
}
