package org.skysongdev.skysongEconomy.business.shift;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Settings from {@code businesses/shifts.yml}.
 *
 * @param duration       how long a shift takes, counting only time online and not AFK
 * @param downtimeCost   downtime spent to start one, from the player's preferred pool
 * @param defaultMessage the shift message of businesses whose owner didn't set one; may use
 *                       {@code {character}}, {@code {player}} and {@code {business}}
 * @param pay            pay by business type id, then tier; a missing entry pays nothing
 */
public record ShiftSettings(Duration duration, int downtimeCost, String defaultMessage,
                            Map<String, Map<Integer, ShiftPay>> pay) {
    public ShiftSettings {
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(defaultMessage, "defaultMessage");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
        if (downtimeCost < 0) {
            throw new IllegalArgumentException("downtimeCost must not be negative: " + downtimeCost);
        }
        pay = Map.copyOf(pay);
    }

    /** @return what a shift at a business of that type and tier pays */
    public ShiftPay pay(String typeId, int tier) {
        return pay.getOrDefault(typeId, Map.of()).getOrDefault(tier, ShiftPay.NONE);
    }
}
