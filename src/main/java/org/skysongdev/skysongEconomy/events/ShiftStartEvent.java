package org.skysongdev.skysongEconomy.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.CharacterRef;

/**
 * A player is about to start a shift at a business. Cancel it to stop the shift; nothing is
 * spent then.
 * <p>
 * {@link #getMessage()} is the business's shift message with its placeholders filled in. It
 * isn't shown in game, so other plugins (such as a Discord bridge) can announce it.
 */
public final class ShiftStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Business business;
    private final Player player;
    private final CharacterRef character;
    private final String message;
    private boolean cancelled;

    public ShiftStartEvent(Business business, Player player, CharacterRef character, String message) {
        this.business = business;
        this.player = player;
        this.character = character;
        this.message = message;
    }

    public Business getBusiness() {
        return business;
    }

    public Player getPlayer() {
        return player;
    }

    /** @return the character working the shift */
    public CharacterRef getCharacter() {
        return character;
    }

    /** @return the shift message, e.g. "Aldric started a shift at The Rusty Anvil" */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
