package org.skysongdev.skysongEconomy.api.business;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.Objects;

/**
 * A player is about to start a shift at a business. Fired on the main thread. Cancel it to stop
 * the shift; nothing is spent then.
 * <p>
 * {@link #getMessage()} is the business's shift message with its placeholders filled in. It
 * isn't shown in game, so other plugins (such as a Discord bridge) can announce it.
 */
public final class ShiftStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final Player player;
    private final CharacterRef character;
    private final String message;
    private boolean cancelled;

    public ShiftStartEvent(@NotNull BusinessInfo business, @NotNull Player player, @NotNull CharacterRef character,
                           @NotNull String message) {
        this.business = Objects.requireNonNull(business, "business");
        this.player = Objects.requireNonNull(player, "player");
        this.character = Objects.requireNonNull(character, "character");
        this.message = Objects.requireNonNull(message, "message");
    }

    /** @return the business, as it was when this fired */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the player starting it */
    public @NotNull Player getPlayer() {
        return player;
    }

    /** @return the character working the shift */
    public @NotNull CharacterRef getCharacter() {
        return character;
    }

    /** @return the shift message, e.g. "Aldric started a shift at The Rusty Anvil" */
    public @NotNull String getMessage() {
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

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
