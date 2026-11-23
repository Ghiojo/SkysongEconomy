package org.skysongdev.skysongEconomy.trade.delivery;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;
import java.util.Optional;

/**
 * Another plugin cancelled an import order ({@code ShipmentOrderEvent}). Nothing was paid. Its
 * {@link #reason()} is MiniMessage that plugin wrote; without one, show {@code business.imports.order-cancelled}.
 */
public final class OrderCancelledException extends RefusedException {
    private static final long serialVersionUID = 1L;

    private final transient Optional<String> reason;

    public OrderCancelledException(Optional<String> reason) {
        super("business.imports.order-cancelled", Map.of());
        this.reason = reason;
    }

    /** @return the cancelling plugin's MiniMessage text for the player, if it gave one */
    public Optional<String> reason() {
        return reason;
    }
}
