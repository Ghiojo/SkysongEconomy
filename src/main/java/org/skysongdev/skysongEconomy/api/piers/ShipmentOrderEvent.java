package org.skysongdev.skysongEconomy.api.piers;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * An import is about to be ordered: every check passed and nothing has been paid yet. Fired on the
 * main thread. Cancelling it stops the order; nothing is paid and the player is told why.
 */
public final class ShipmentOrderEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Shipment shipment;
    private final Optional<Pier> pier;
    private final @Nullable Player player;
    private boolean cancelled;
    private @Nullable String cancelMessage;

    /**
     * @param shipment what would be ordered, with the id it will have
     * @param pier     the pier it would be sent to; empty when there are no piers
     * @param player   the player ordering, when online
     */
    public ShipmentOrderEvent(@NotNull Shipment shipment, @NotNull Optional<Pier> pier, @Nullable Player player) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.pier = Objects.requireNonNull(pier, "pier");
        this.player = player;
    }

    /** @return what would be ordered: business, ledger, import, pier, items, price, tags */
    public @NotNull Shipment getShipment() {
        return shipment;
    }

    /** @return the pier it would be sent to; empty when there are no piers */
    public @NotNull Optional<Pier> getPier() {
        return pier;
    }

    /** @return the player ordering; empty if they aren't online */
    public @NotNull Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /** @return what the player is told when it is cancelled; empty for messages.yml's {@code business.imports.order-cancelled} */
    public @NotNull Optional<String> getCancelMessage() {
        return Optional.ofNullable(cancelMessage);
    }

    /** @param message MiniMessage text shown to the player when it is cancelled; {@code null} for the default */
    public void setCancelMessage(@Nullable String message) {
        this.cancelMessage = message;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
