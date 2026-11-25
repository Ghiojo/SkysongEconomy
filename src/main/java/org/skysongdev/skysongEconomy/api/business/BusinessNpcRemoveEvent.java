package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A business no longer has one of its NPCs, and the slot it filled is free. Fired on the main
 * thread. Unless the cause is {@link NpcRemovalCause#REMOVED_ELSEWHERE}, the NPC still exists in
 * SkysongNPCs while this fires and is removed right after.
 */
public final class BusinessNpcRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final BusinessNpcInfo npc;
    private final NpcRemovalCause cause;

    public BusinessNpcRemoveEvent(@NotNull BusinessInfo business, @NotNull BusinessNpcInfo npc,
                                  @NotNull NpcRemovalCause cause) {
        this.business = Objects.requireNonNull(business, "business");
        this.npc = Objects.requireNonNull(npc, "npc");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the NPC the business had: its SkysongNPCs id and the slot it filled */
    public @NotNull BusinessNpcInfo getNpc() {
        return npc;
    }

    /** @return why the business no longer has it */
    public @NotNull NpcRemovalCause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
