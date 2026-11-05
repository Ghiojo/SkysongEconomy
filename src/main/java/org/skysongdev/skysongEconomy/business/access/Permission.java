package org.skysongdev.skysongEconomy.business.access;

import java.util.Locale;
import java.util.Optional;

/** Something an access group may let its members do at a business. The owner may do them all. */
public enum Permission {
    /** Use Train/Learn, at the group's training price. */
    TRAIN,
    /** Order imports (Economy and Logistics). */
    ORDER_IMPORTS,
    /** Sell exports (Economy and Logistics). */
    SELL_EXPORTS;

    /** @return the name used in config and commands, e.g. {@code order-imports} */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** @return the permission with that config name */
    public static Optional<Permission> fromKey(String key) {
        for (Permission permission : values()) {
            if (permission.key().equalsIgnoreCase(key)) {
                return Optional.of(permission);
            }
        }
        return Optional.empty();
    }
}
