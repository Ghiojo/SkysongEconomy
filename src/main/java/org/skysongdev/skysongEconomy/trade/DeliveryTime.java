package org.skysongdev.skysongEconomy.trade;

import java.time.Duration;
import java.util.Objects;

/**
 * One of the server's delivery options, such as Express (24 hours). Imports set a price for
 * each delivery time they offer.
 *
 * @param id          the key in config, e.g. {@code express}
 * @param displayName MiniMessage name shown in menus
 * @param duration    real time from ordering to arrival, which also passes while everyone is offline
 */
public record DeliveryTime(String id, String displayName, Duration duration) {
    public DeliveryTime {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
    }
}
