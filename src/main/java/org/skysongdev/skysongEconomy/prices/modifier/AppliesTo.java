package org.skysongdev.skysongEconomy.prices.modifier;

import java.util.Locale;
import java.util.Optional;

/** Which prices a modifier changes. Imports are the default; exports are opt-in. */
public enum AppliesTo {
    IMPORTS, EXPORTS, BOTH;

    /** @return whether a modifier with this setting changes a price in that direction */
    public boolean covers(TradeDirection direction) {
        return switch (this) {
            case IMPORTS -> direction == TradeDirection.IMPORT;
            case EXPORTS -> direction == TradeDirection.EXPORT;
            case BOTH -> true;
        };
    }

    /** @return the word used in commands and storage, e.g. {@code imports} */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the setting written as {@code imports}, {@code exports} or {@code both} */
    public static Optional<AppliesTo> fromKey(String key) {
        for (AppliesTo value : values()) {
            if (value.key().equalsIgnoreCase(key)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
