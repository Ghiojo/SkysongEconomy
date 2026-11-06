package org.skysongdev.skysongEconomy.nodes;

import java.util.Locale;
import java.util.Optional;

/** The two kinds of gathering node. */
public enum NodeKind {
    /** Rolls on a random table ({@code nodes/random/<id>.yml}): mines, forestry and the like. */
    RANDOM,
    /** Grows whatever primer item is planted in it, from one farm type ({@code nodes/farm/types/<id>.yml}). */
    FARM;

    /** @return the name used in commands and storage, e.g. {@code random} */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the kind with that name, ignoring case */
    public static Optional<NodeKind> fromKey(String key) {
        for (NodeKind kind : values()) {
            if (kind.key().equalsIgnoreCase(key)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
