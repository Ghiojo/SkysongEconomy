package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.config.FileEntry;
import org.skysongdev.skysongEconomy.loot.YieldEffect;

import java.util.Objects;

/**
 * One file of {@code nodes/modifiers}: while its window holds today's IC date, harvests on the
 * nodes it targets are changed by its effect, or can't happen at all when it is dormant.
 *
 * @param id      the file name
 * @param name    what players see, in MiniMessage
 * @param window  when it is on
 * @param target  which nodes and tiers it hits
 * @param effect  what it changes; {@link YieldEffect#NONE} when dormant
 * @param dormant whether harvests are stopped while it is on
 */
public record CalendarModifier(String id, String name, CalendarWindow window, NodeTarget target, YieldEffect effect,
                               boolean dormant) implements FileEntry {
    public CalendarModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(effect, "effect");
    }
}
