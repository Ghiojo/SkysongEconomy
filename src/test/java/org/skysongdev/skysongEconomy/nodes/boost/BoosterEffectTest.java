package org.skysongdev.skysongEconomy.nodes.boost;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.IntRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoosterEffectTest {
    @Test
    void readsEveryKindOfPart() {
        assertEquals(new BoosterEffect(0, 1, 25, 0, 1), BoosterEffect.parse("min+1; max+25%; rolls+1"));
        assertEquals(new BoosterEffect(-10, 0, 0, -2, -1), BoosterEffect.parse("MIN - 10% ;max-2; rolls-1;"));
    }

    @Test
    void addsUpPartsOnTheSameThing() {
        assertEquals(new BoosterEffect(0, 0, 25, 3, 0), BoosterEffect.parse("max+10%; max+15%; max+1; max+2"));
    }

    @Test
    void refusesWhatItDoesNotUnderstand() {
        assertThrows(IllegalArgumentException.class, () -> BoosterEffect.parse(""));
        assertThrows(IllegalArgumentException.class, () -> BoosterEffect.parse("  ; "));
        assertThrows(IllegalArgumentException.class, () -> BoosterEffect.parse("luck+1"));
        assertThrows(IllegalArgumentException.class, () -> BoosterEffect.parse("max*2"));
        assertThrows(IllegalArgumentException.class, () -> BoosterEffect.parse("max+1.5"));
        IllegalArgumentException rolls = assertThrows(IllegalArgumentException.class, () -> BoosterEffect.parse("rolls+50%"));
        assertTrue(rolls.getMessage().contains("rolls+50%"));
    }

    @Test
    void appliesPercentFirstThenFlatAndTruncates() {
        // 3 * 150% = 4.5, truncated to 4, then +1.
        assertEquals(new IntRange(2, 5), BoosterEffect.parse("max+50%; max+1").amount(new IntRange(2, 3)));
        assertEquals(new IntRange(3, 3), BoosterEffect.parse("min+1").amount(new IntRange(2, 3)));
    }

    @Test
    void neverGoesBelowZeroAndKeepsMinAtMostMax() {
        assertEquals(new IntRange(0, 0), BoosterEffect.parse("min-5; max-5").amount(new IntRange(1, 3)));
        assertEquals(new IntRange(4, 4), BoosterEffect.parse("min+10").amount(new IntRange(1, 4)));
        assertEquals(new IntRange(1, 1), BoosterEffect.parse("max-90%").amount(new IntRange(1, 10)));
    }

    @Test
    void changesRollCountsButNotBelowZero() {
        BoosterEffect effect = BoosterEffect.parse("rolls+2");
        assertEquals(5, effect.rolls(3));
        assertEquals(0, BoosterEffect.parse("rolls-4").rolls(3));
    }

    @Test
    void noneChangesNothing() {
        assertEquals(new IntRange(2, 7), BoosterEffect.NONE.amount(new IntRange(2, 7)));
        assertEquals(3, BoosterEffect.NONE.rolls(3));
    }
}
