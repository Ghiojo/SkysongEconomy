package org.skysongdev.skysongEconomy.business.scale;

import java.util.Locale;
import java.util.Optional;

/** Which of a business's two numbers a table follows. */
public enum Basis {
    /** How many of the business's skill picks are the skill in question (the skill pick tier). */
    LEVEL,
    /** The business's overall tier. */
    TIER;

    /** @return the name used in config files and formulas, {@code level} or {@code tier} */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the basis with that config name, ignoring case */
    public static Optional<Basis> fromKey(String key) {
        for (Basis basis : values()) {
            if (basis.key().equalsIgnoreCase(key.strip())) {
                return Optional.of(basis);
            }
        }
        return Optional.empty();
    }
}
