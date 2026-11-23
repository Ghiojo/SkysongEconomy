package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.loot.YieldEffect;

import java.util.List;
import java.util.Optional;

/**
 * The calendar modifiers hitting one harvest today.
 *
 * @param applied every modifier on that hits it, in file name order
 */
public record ActiveModifiers(List<CalendarModifier> applied) {
    /** None: nothing changes. */
    public static final ActiveModifiers NONE = new ActiveModifiers(List.of());

    public ActiveModifiers {
        applied = List.copyOf(applied);
    }

    /** @return every effect added up; {@link YieldEffect#NONE} with none */
    public YieldEffect effect() {
        return applied.stream().map(CalendarModifier::effect).reduce(YieldEffect.NONE, YieldEffect::plus);
    }

    /** @return the first dormant modifier, which stops the harvest; empty when none is dormant */
    public Optional<CalendarModifier> dormant() {
        return applied.stream().filter(CalendarModifier::dormant).findFirst();
    }

    /** @return the ids of the modifiers, for the harvest event */
    public List<String> ids() {
        return applied.stream().map(CalendarModifier::id).toList();
    }

    public boolean isEmpty() {
        return applied.isEmpty();
    }
}
