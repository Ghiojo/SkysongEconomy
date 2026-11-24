package org.skysongdev.skysongEconomy.api.piers;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongEconomy.api.business.BusinessInfo;

import java.util.Objects;

/**
 * A business was deleted while one of its imports was still on its way or waiting at its pier,
 * so SkysongEconomy gave its price back to whoever paid and the shipment is gone. Fired on the main
 * thread, once per shipment. It can't be cancelled.
 */
public final class ShipmentRefundedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BusinessInfo business;
    private final Shipment shipment;
    private final boolean paidBack;

    public ShipmentRefundedEvent(@NotNull BusinessInfo business, @NotNull Shipment shipment, boolean paidBack) {
        this.business = Objects.requireNonNull(business, "business");
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.paidBack = paidBack;
    }

    /** @return the deleted business, as it was */
    public @NotNull BusinessInfo getBusiness() {
        return business;
    }

    /** @return the shipment that was refunded */
    public @NotNull Shipment getShipment() {
        return shipment;
    }

    /** @return whether the money reached the orderer; {@code false} when the payment failed, which SkysongEconomy logs as a warning */
    public boolean isPaidBack() {
        return paidBack;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
