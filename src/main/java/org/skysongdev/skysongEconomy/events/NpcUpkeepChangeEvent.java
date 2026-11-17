package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.upkeep.NpcUpkeep;
import org.skysongdev.skysongEconomy.upkeep.UpkeepEvents.Cause;
import org.skysongdev.skysongEconomy.upkeep.UpkeepState;

import java.util.UUID;

/**
 * An NPC became maintained or unmaintained. Unmaintained is only a marker: SkysongEconomy changes
 * nothing else about the NPC, so a plugin that wants it to mean more listens here. Fired on the
 * main thread, after the change is saved.
 */
public final class NpcUpkeepChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final NpcUpkeep upkeep;
    private final Cause cause;

    public NpcUpkeepChangeEvent(NpcUpkeep upkeep, Cause cause) {
        this.upkeep = upkeep;
        this.cause = cause;
    }

    /** @return the NPC's Citizens UUID */
    public UUID getNpc() {
        return upkeep.npc();
    }

    /** @return its state now */
    public UpkeepState getState() {
        return upkeep.state();
    }

    /** @return its whole upkeep now: profile, state, next cycle */
    public NpcUpkeep getUpkeep() {
        return upkeep;
    }

    /** @return why it changed */
    public Cause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
