package org.skysongdev.skysongEconomy.trade.delivery;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.api.piers.Shipment;

import java.util.Optional;

/**
 * Tells other plugins about shipments and lets them step in. The delivery rules call it; the
 * server's version fires the Bukkit events in {@code api/piers}, and tests use a fake.
 */
public interface ShipmentEvents {
    /** What a listener decided about an order. */
    record Verdict(boolean cancelled, Optional<String> message) {
        /** Go ahead. */
        public static final Verdict ALLOW = new Verdict(false, Optional.empty());

        /** @param message MiniMessage text for the player, or {@code null} for the default */
        public static Verdict cancel(@Nullable String message) {
            return new Verdict(true, Optional.ofNullable(message));
        }
    }

    /** Fires nothing and allows everything. */
    ShipmentEvents NONE = shipment -> Verdict.ALLOW;

    /** An import is about to be ordered, before anything is paid. */
    Verdict ordering(Shipment shipment);
}
