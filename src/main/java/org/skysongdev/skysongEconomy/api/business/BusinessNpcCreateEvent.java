package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A business created an NPC in one of its slots. Fired on the main thread. The NPC exists in
 * SkysongNPCs already, owned by the business's owner, and is waiting for its first setup.
 */
public final class BusinessNpcCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final BusinessNpcInfo npc;

    public BusinessNpcCreateEvent(@NotNull BusinessInfo business, @NotNull BusinessNpcInfo npc) {
        this.business = Objects.requireNonNull(business, "business");
        this.npc = Objects.requireNonNull(npc, "npc");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the new NPC: its SkysongNPCs id and the slot it fills */
    public @NotNull BusinessNpcInfo getNpc() {
        return npc;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
