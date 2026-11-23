package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.FixedCalendar;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.loot.YieldEffect;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeModifiersTest {
    private static final NodeFacts FIELD = NodeFacts.farm("field", "crop", "wheat");

    /** A modifier on in Vinse'er for crop farms. */
    static CalendarModifier winter(String id, String effect) {
        return new CalendarModifier(id, id, new CalendarWindow(Set.of("vinse_er"), List.of(), false),
                new NodeTarget(Set.of(), Set.of(), Set.of("crop"), Set.of(), Set.of(), Set.of()),
                effect.equals("dormant") ? YieldEffect.NONE : YieldEffect.parse(effect), effect.equals("dormant"));
    }

    @Test
    void everyModifierOnTodayThatHitsTheNodeAddsUp() {
        FileRegistry<CalendarModifier> files = new FileRegistry<>(List.of(winter("frost", "max-50%"),
                winter("short-days", "rolls-1; max+10%")));
        NodeModifiers modifiers = new NodeModifiers(() -> files, FixedCalendar.on("rana", 3));

        ActiveModifiers active = modifiers.activeFor(FIELD, 1);

        assertEquals(List.of("frost", "short-days"), active.ids());
        assertEquals(YieldEffect.parse("max-40%; rolls-1"), active.effect());
        assertTrue(active.dormant().isEmpty());
    }

    @Test
    void nothingIsOnOutsideTheWindowOrForOtherNodes() {
        FileRegistry<CalendarModifier> files = new FileRegistry<>(List.of(winter("frost", "max-50%")));
        FixedCalendar calendar = FixedCalendar.on("celestine", 3);
        NodeModifiers modifiers = new NodeModifiers(() -> files, calendar);

        assertTrue(modifiers.activeFor(FIELD, 1).isEmpty(), "Florescia");
        calendar.set(FixedCalendar.dayOf("rana", 1));
        assertTrue(modifiers.activeFor(NodeFacts.random("quarry", "mine", "mining"), 1).isEmpty());
        assertEquals(1, modifiers.activeFor(FIELD, 1).applied().size(), "a new day applies at once");
    }

    @Test
    void aDormantModifierIsReported() {
        FileRegistry<CalendarModifier> files = new FileRegistry<>(List.of(winter("frost", "max-50%"),
                winter("asleep", "dormant")));
        NodeModifiers modifiers = new NodeModifiers(() -> files, FixedCalendar.on("rana", 3));

        assertEquals("asleep", modifiers.activeFor(FIELD, 1).dormant().orElseThrow().id());
    }

    @Test
    void withoutACalendarNothingIsEverOn() {
        FileRegistry<CalendarModifier> files = new FileRegistry<>(List.of(winter("frost", "max-50%")));
        NodeModifiers modifiers = new NodeModifiers(() -> files, CalendarClock.NONE);

        assertEquals(ActiveModifiers.NONE, modifiers.activeFor(FIELD, 1));
        assertTrue(modifiers.activeOn(CalendarDay.of("vinse_er", "rana", 1)).isEmpty());
        assertEquals(ActiveModifiers.NONE, NodeModifiers.NONE.activeFor(FIELD, 1));
    }
}
