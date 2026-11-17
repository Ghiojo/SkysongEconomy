package org.skysongdev.skysongEconomy.prices.modifier;

import java.util.Locale;
import java.util.Optional;

/**
 * The kinds of {@link ModifierTarget}, as written before the colon ({@code global} has none).
 * Tax-exempt ledgers list the kinds they ignore by these keys.
 */
public enum TargetKind {
    GLOBAL("global"),
    BUSINESS("business"),
    LEDGER("ledger"),
    GOOD_TYPE("good-type"),
    ITEM("item");

    private final String key;

    TargetKind(String key) {
        this.key = key;
    }

    /** @return the kind as commands and files write it, e.g. {@code good-type} */
    public String key() {
        return key;
    }

    /** @return the kind with that key, ignoring case, or empty when there is none */
    public static Optional<TargetKind> parse(String text) {
        String lower = text.strip().toLowerCase(Locale.ROOT);
        for (TargetKind kind : values()) {
            if (kind.key.equals(lower)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
