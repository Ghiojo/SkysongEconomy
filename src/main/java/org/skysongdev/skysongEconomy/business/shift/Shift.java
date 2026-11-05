package org.skysongdev.skysongEconomy.business.shift;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One shift, from its start until its pay is retrieved. Immutable: progress makes a new copy.
 *
 * @param id         unique, generated when it starts
 * @param businessId where it is worked
 * @param character  who works it; the money goes to their player
 * @param startedAt  when it started
 * @param required   how much online, non-AFK time it takes
 * @param worked     how much of that is done
 * @param pay        what it pays, fixed when it started
 * @param vaultCut   what it adds to the vault when it ends; 0 for the owner's own shifts
 * @param finishedAt when it ended, {@code null} while it is being worked
 */
public record Shift(UUID id, String businessId, CharacterRef character, Instant startedAt, Duration required,
                    Duration worked, ShiftPay pay, BigDecimal vaultCut, @Nullable Instant finishedAt) {
    public Shift {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(worked, "worked");
        Objects.requireNonNull(pay, "pay");
        Objects.requireNonNull(vaultCut, "vaultCut");
    }

    /** @return a shift that has just started */
    public static Shift start(String businessId, CharacterRef character, Instant now, Duration required,
                              ShiftPay pay, BigDecimal vaultCut) {
        return new Shift(UUID.randomUUID(), businessId, character, now, required, Duration.ZERO, pay, vaultCut, null);
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public Optional<Instant> finished() {
        return Optional.ofNullable(finishedAt);
    }

    /** @return how much time is left, never negative */
    public Duration remaining() {
        Duration left = required.minus(worked);
        return left.isNegative() ? Duration.ZERO : left;
    }

    /** @return this shift with more time worked, ended at {@code now} if that was enough */
    Shift work(Duration time, Instant now) {
        if (isFinished()) {
            return this;
        }
        Duration total = worked.plus(time);
        boolean done = total.compareTo(required) >= 0;
        return new Shift(id, businessId, character, startedAt, required, done ? required : total, pay, vaultCut,
                done ? now : null);
    }
}
