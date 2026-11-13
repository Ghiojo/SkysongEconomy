package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;

/**
 * A business created an NPC in one of its slots. The NPC exists in SkysongNPCs already, owned by
 * the business's owner, and is waiting for its first setup.
 */
public final class BusinessNpcCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Business business;
    private final BusinessNpc npc;

    public BusinessNpcCreateEvent(Business business, BusinessNpc npc) {
        this.business = business;
        this.npc = npc;
    }

    public Business getBusiness() {
        return business;
    }

    /** @return the new NPC: its SkysongNPCs id and the slot it fills */
    public BusinessNpc getNpc() {
        return npc;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
