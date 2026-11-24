package org.skysongdev.skysongEconomy.api.piers;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;
import org.skysongdev.skysongEconomy.api.trade.TradePrice;

import java.util.Objects;
import java.util.Optional;

/**
 * An import was ordered: the orderer paid and a delivery slot is booked. Fired on the main thread,
 * after {@link ShipmentOrderEvent} let it through. It can't be cancelled; it is meant for logs,
 * books and statistics. The money it cost left the economy.
 */
public final class ShipmentOrderedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final Shipment shipment;
    private final Optional<Pier> pier;
    private final TradePrice price;
    private final @Nullable Player player;

    public ShipmentOrderedEvent(@NotNull BusinessInfo business, @NotNull Shipment shipment,
                                @NotNull Optional<Pier> pier, @NotNull TradePrice price, @Nullable Player player) {
        this.business = Objects.requireNonNull(business, "business");
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.pier = Objects.requireNonNull(pier, "pier");
        this.price = Objects.requireNonNull(price, "price");
        this.player = player;
    }

    /** @return the business that ordered it */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return what was ordered: ledger, import, pier, items, price, tags */
    public @NotNull Shipment getShipment() {
        return shipment;
    }

    /** @return the pier it is sent to; empty when there are no piers */
    public @NotNull Optional<Pier> getPier() {
        return pier;
    }

    /** @return what was paid: the base, every modifier with its share, and the total */
    public @NotNull TradePrice getPrice() {
        return price;
    }

    /** @return the player who ordered, while online */
    public @NotNull Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
