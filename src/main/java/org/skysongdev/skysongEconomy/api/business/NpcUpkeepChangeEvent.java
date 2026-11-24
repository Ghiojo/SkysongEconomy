package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * An NPC became maintained or unmaintained. Unmaintained is only a marker: SkysongEconomy changes
 * nothing else about the NPC, so a plugin that wants it to mean more listens here. Fired on the
 * main thread, after the change is saved.
 */
public final class NpcUpkeepChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final NpcUpkeepInfo upkeep;
    private final UpkeepChangeCause cause;

    public NpcUpkeepChangeEvent(@NotNull NpcUpkeepInfo upkeep, @NotNull UpkeepChangeCause cause) {
        this.upkeep = Objects.requireNonNull(upkeep, "upkeep");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** @return its whole upkeep now: profile, state, next cycle */
    public @NotNull NpcUpkeepInfo getUpkeep() {
        return upkeep;
    }

    /** @return why it changed */
    public @NotNull UpkeepChangeCause getCause() {
        return cause;
    }

    /** @return the NPC's Citizens UUID */
    public @NotNull UUID getNpc() {
        return upkeep.npc();
    }

    /** @return whether it is maintained now */
    public boolean isMaintained() {
        return upkeep.maintained();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
