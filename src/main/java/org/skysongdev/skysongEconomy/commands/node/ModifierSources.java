package org.skysongdev.skysongEconomy.commands.node;

import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.modifiers.ActiveModifiers;
import org.skysongdev.skysongEconomy.nodes.modifiers.NodeModifiers;

import java.util.Objects;
import java.util.function.Function;

/**
 * What {@code /se node modifiers} and {@code /se node odds} read about calendar modifiers.
 *
 * @param modifiers the loaded calendar modifiers
 * @param calendar  the IC calendar; {@link CalendarClock#NONE} without SkysongCalendar
 * @param onNode    the modifiers on a node today, at any tier it offers
 */
public record ModifierSources(NodeModifiers modifiers, CalendarClock calendar, Function<Node, ActiveModifiers> onNode) {
    public ModifierSources {
        Objects.requireNonNull(modifiers, "modifiers");
        Objects.requireNonNull(calendar, "calendar");
        Objects.requireNonNull(onNode, "onNode");
    }
}
