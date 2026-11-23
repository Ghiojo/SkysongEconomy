package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.calendar.CalendarClock;
import org.skysongdev.skysongEconomy.calendar.CalendarDay;
import org.skysongdev.skysongEconomy.calendar.CalendarMoment;
import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.config.FileRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Finds the calendar modifiers on for a harvest: the loaded files whose window holds today's IC
 * date and whose target names the node and tier, unless staff turned modifiers off for the node.
 * It reads the date and the loaded files on every call, so a reload or a new day applies at once.
 * Without a calendar nothing is ever on.
 */
public final class NodeModifiers {
    /** No modifiers: harvests are never changed. */
    public static final NodeModifiers NONE = new NodeModifiers(FileRegistry::empty, CalendarClock.NONE);

    private final Supplier<FileRegistry<CalendarModifier>> modifiers;
    private final CalendarClock calendar;

    /**
     * @param modifiers the loaded modifier files, looked up on every call
     * @param calendar  where today's IC date comes from
     */
    public NodeModifiers(Supplier<FileRegistry<CalendarModifier>> modifiers, CalendarClock calendar) {
        this.modifiers = Objects.requireNonNull(modifiers, "modifiers");
        this.calendar = Objects.requireNonNull(calendar, "calendar");
    }

    /** @return the modifiers hitting a harvest at {@code tier} on that node today */
    public ActiveModifiers activeFor(NodeFacts node, int tier) {
        Optional<CalendarDay> today = calendar.today();
        return today.isEmpty() ? ActiveModifiers.NONE : activeFor(node, tier, today.get());
    }

    /**
     * @return the modifiers that would hit a harvest at {@code tier} on that node on {@code day};
     *         none when the node has them turned off
     */
    public ActiveModifiers activeFor(NodeFacts node, int tier, CalendarDay day) {
        if (!node.enabled()) {
            return ActiveModifiers.NONE;
        }
        return new ActiveModifiers(activeOn(day).stream().filter(modifier -> modifier.target().matches(node, tier))
                .toList());
    }

    /**
     * @return the modifiers hitting the node today at any tier up to {@code maxTier}, such as for
     *         its nameplate; none when the node has them turned off
     */
    public ActiveModifiers activeUpTo(NodeFacts node, int maxTier) {
        Optional<CalendarDay> today = calendar.today();
        if (today.isEmpty() || !node.enabled()) {
            return ActiveModifiers.NONE;
        }
        return new ActiveModifiers(activeOn(today.get()).stream().filter(modifier ->
                IntStream.rangeClosed(1, maxTier).anyMatch(tier -> modifier.target().matches(node, tier))).toList());
    }

    /**
     * @return the modifiers that would hit a harvest at {@code tier} on that node at that moment,
     *         such as for {@code /se node odds}; none when the node has them turned off
     */
    public ActiveModifiers activeFor(NodeFacts node, int tier, CalendarMoment moment) {
        Optional<CalendarNames> names = calendar.names();
        if (names.isEmpty() || !node.enabled()) {
            return ActiveModifiers.NONE;
        }
        return new ActiveModifiers(modifiers.get().all().stream()
                .filter(modifier -> modifier.window().covers(moment, names.get()))
                .filter(modifier -> modifier.target().matches(node, tier)).toList());
    }

    /** @return every modifier on, on {@code day}, whatever it hits; none without a calendar */
    public List<CalendarModifier> activeOn(CalendarDay day) {
        Optional<CalendarNames> names = calendar.names();
        if (names.isEmpty()) {
            return List.of();
        }
        return modifiers.get().all().stream().filter(modifier -> modifier.window().contains(day, names.get())).toList();
    }
}
