package org.skysongdev.skysongEconomy.prices.modifier;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * A modifier before it is saved: everything but its id and creation time.
 *
 * @param amount      a percentage ({@code +10%}) or, for Claims, a flat amount ({@code +50})
 * @param displayName shown text, or {@code null} for one made from the type and amount
 * @param expiresAt   when it stops applying, or {@code null} for never
 */
public record ModifierDraft(String type, ModifierTarget target, AppliesTo appliesTo, ModifierAmount amount,
                            @Nullable String displayName, @Nullable Instant expiresAt, String createdBy) {
    public ModifierDraft {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(appliesTo, "appliesTo");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(createdBy, "createdBy");
    }
}
