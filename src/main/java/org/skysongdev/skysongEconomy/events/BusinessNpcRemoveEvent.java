package org.skysongdev.skysongEconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcEvents.RemovalCause;

/**
 * A business no longer has one of its NPCs, and the slot it filled is free. Unless the cause is
 * {@link RemovalCause#REMOVED_ELSEWHERE}, the NPC still exists in SkysongNPCs while this fires
 * and is removed right after.
 */
public final class BusinessNpcRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Business business;
    private final BusinessNpc npc;
    private final RemovalCause cause;

    public BusinessNpcRemoveEvent(Business business, BusinessNpc npc, RemovalCause cause) {
        this.business = business;
        this.npc = npc;
        this.cause = cause;
    }

    public Business getBusiness() {
        return business;
    }

    /** @return the NPC the business had: its SkysongNPCs id and the slot it filled */
    public BusinessNpc getNpc() {
        return npc;
    }

    /** @return why the business no longer has it */
    public RemovalCause getCause() {
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
