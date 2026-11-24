package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.api.items.ItemCount;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * An NPC's upkeep cycle came and its cost was eaten from the NPC's stock. Fired on the main thread,
 * once per cycle: cycles missed while the server was down each fire, in order. A cycle that
 * couldn't be paid fires {@link NpcUpkeepChangeEvent} (the NPC becomes unmaintained) instead.
 */
public final class NpcUpkeepPaidEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final NpcUpkeepInfo upkeep;
    private final Instant cycleDue;
    private final List<ItemCount> eaten;

    public NpcUpkeepPaidEvent(@NotNull NpcUpkeepInfo upkeep, @NotNull Instant cycleDue,
                              @NotNull List<ItemCount> eaten) {
        this.upkeep = Objects.requireNonNull(upkeep, "upkeep");
        this.cycleDue = Objects.requireNonNull(cycleDue, "cycleDue");
        this.eaten = List.copyOf(Objects.requireNonNull(eaten, "eaten"));
    }

    /** @return the NPC's upkeep after paying, with its next cycle */
    public @NotNull NpcUpkeepInfo getUpkeep() {
        return upkeep;
    }

    /** @return when the paid cycle was due */
    public @NotNull Instant getCycleDue() {
        return cycleDue;
    }

    /** @return the items taken from the stock, each material once */
    public @NotNull List<ItemCount> getEaten() {
        return eaten;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
