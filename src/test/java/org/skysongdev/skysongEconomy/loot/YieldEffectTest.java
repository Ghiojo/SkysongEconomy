package org.skysongdev.skysongEconomy.loot;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.trade.IntRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YieldEffectTest {
    @Test
    void readsEveryKindOfPart() {
        assertEquals(new YieldEffect(0, 1, 25, 0, 1, 0), YieldEffect.parse("min+1; max+25%; rolls+1"));
        assertEquals(new YieldEffect(-10, 0, 0, -2, -1, 0), YieldEffect.parse("MIN - 10% ;max-2; rolls-1;"));
    }

    @Test
    void addsUpPartsOnTheSameThing() {
        assertEquals(new YieldEffect(0, 0, 25, 3, 0, 0), YieldEffect.parse("max+10%; max+15%; max+1; max+2"));
    }

    @Test
    void refusesWhatItDoesNotUnderstand() {
        assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse(""));
        assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse("  ; "));
        assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse("luck+1"));
        assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse("chance+5"));
        assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse("max*2"));
        assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse("max+1.5"));
        IllegalArgumentException rolls = assertThrows(IllegalArgumentException.class, () -> YieldEffect.parse("rolls+50%"));
        assertTrue(rolls.getMessage().contains("rolls+50%"));
    }

    @Test
    void appliesPercentFirstThenFlatAndTruncates() {
        // 3 * 150% = 4.5, truncated to 4, then +1.
        assertEquals(new IntRange(2, 5), YieldEffect.parse("max+50%; max+1").amount(new IntRange(2, 3)));
        assertEquals(new IntRange(3, 3), YieldEffect.parse("min+1").amount(new IntRange(2, 3)));
    }

    @Test
    void neverGoesBelowZeroAndKeepsMinAtMostMax() {
        assertEquals(new IntRange(0, 0), YieldEffect.parse("min-5; max-5").amount(new IntRange(1, 3)));
        assertEquals(new IntRange(4, 4), YieldEffect.parse("min+10").amount(new IntRange(1, 4)));
        assertEquals(new IntRange(1, 1), YieldEffect.parse("max-90%").amount(new IntRange(1, 10)));
    }

    @Test
    void changesRollCountsButNotBelowZero() {
        YieldEffect effect = YieldEffect.parse("rolls+2");
        assertEquals(5, effect.rolls(3));
        assertEquals(0, YieldEffect.parse("rolls-4").rolls(3));
    }

    @Test
    void noneChangesNothing() {
        assertEquals(new IntRange(2, 7), YieldEffect.NONE.amount(new IntRange(2, 7)));
        assertEquals(3, YieldEffect.NONE.rolls(3));
    }

    @Test
    void changesChancesInPercentOfThemselvesBetweenZeroAndAHundred() {
        assertEquals(new YieldEffect(0, 0, 0, 0, 0, -50), YieldEffect.parse("chance-50%"));
        assertEquals(10, YieldEffect.parse("chance-50%").chance(20), 1e-9);
        assertEquals(30, YieldEffect.parse("chance+50%").chance(20), 1e-9);
        assertEquals(100, YieldEffect.parse("chance+500%").chance(40), 1e-9);
        assertEquals(0, YieldEffect.parse("chance-150%").chance(40), 1e-9);
        assertEquals(40, YieldEffect.NONE.chance(40), 1e-9);
    }

    @Test
    void effectsAddUpPartByPart() {
        YieldEffect winter = YieldEffect.parse("max-50%; rolls-1; chance-50%");
        YieldEffect booster = YieldEffect.parse("max+25%; min+1");

        assertEquals(YieldEffect.parse("max-25%; min+1; rolls-1; chance-50%"), winter.plus(booster));
        assertEquals(winter, winter.plus(YieldEffect.NONE));
    }

    @Test
    void writesItselfBackTheWayItIsRead() {
        YieldEffect effect = YieldEffect.parse("rolls-1; max-50%; min+1; chance+10%");

        assertEquals("min+1; max-50%; rolls-1; chance+10%", effect.text());
        assertEquals(effect, YieldEffect.parse(effect.text()));
        assertEquals("", YieldEffect.NONE.text());
        assertTrue(YieldEffect.NONE.isNone());
    }
}
