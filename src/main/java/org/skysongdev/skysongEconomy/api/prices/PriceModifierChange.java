package org.skysongdev.skysongEconomy.api.prices;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A price modifier that started or stopped changing prices.
 *
 * @param kind     what happened
 * @param modifier the modifier: its new version when added or replaced, or the one that went
 * @param previous the version it replaced; only for {@link Kind#REPLACED}
 * @param at       when it happened
 */
public record PriceModifierChange(Kind kind, PriceModifierInfo modifier, Optional<PriceModifierInfo> previous,
                                  Instant at) {
    public PriceModifierChange {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(at, "at");
    }

    /** What happened to the modifier. New kinds may be added. */
    public enum Kind {
        /** A new modifier was set. */
        ADDED,
        /** A modifier of the same type on the same target was set again; it keeps its id. */
        REPLACED,
        /** Staff removed it, or its target went away (such as a deleted business or pier). */
        REMOVED,
        /** Its expiry time passed. */
        EXPIRED
    }
}
