package org.skysongdev.skysongEconomy.prices.modifier;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A modifier before it is saved: everything but its id and creation time.
 *
 * @param displayName shown text, or {@code null} for one made from the type and percent
 * @param expiresAt   when it stops applying, or {@code null} for never
 */
public record ModifierDraft(String type, ModifierTarget target, AppliesTo appliesTo, BigDecimal percent,
                            @Nullable String displayName, @Nullable Instant expiresAt, String createdBy) {
    public ModifierDraft {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(appliesTo, "appliesTo");
        Objects.requireNonNull(percent, "percent");
        Objects.requireNonNull(createdBy, "createdBy");
    }
}
