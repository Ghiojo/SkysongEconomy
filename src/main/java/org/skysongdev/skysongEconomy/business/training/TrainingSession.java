package org.skysongdev.skysongEconomy.business.training;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A training session being worked. Immutable: progress makes a new copy. It is deleted once it
 * ends and its rewards are given.
 *
 * @param id         unique, generated when it starts
 * @param businessId where it is trained
 * @param character  who trains
 * @param startedAt  when it started
 * @param required   how much online, non-AFK time it takes
 * @param worked     how much of that is done
 * @param price      what the character paid for it
 */
public record TrainingSession(UUID id, String businessId, CharacterRef character, Instant startedAt,
                              Duration required, Duration worked, BigDecimal price) {
    public TrainingSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(worked, "worked");
        Objects.requireNonNull(price, "price");
    }

    /** @return a session that has just started */
    public static TrainingSession start(String businessId, CharacterRef character, Instant now, Duration required,
                                        BigDecimal price) {
        return new TrainingSession(UUID.randomUUID(), businessId, character, now, required, Duration.ZERO, price);
    }

    public boolean isDone() {
        return worked.compareTo(required) >= 0;
    }

    /** @return how much time is left, never negative */
    public Duration remaining() {
        Duration left = required.minus(worked);
        return left.isNegative() ? Duration.ZERO : left;
    }

    TrainingSession work(Duration time) {
        Duration total = worked.plus(time);
        return new TrainingSession(id, businessId, character, startedAt, required,
                total.compareTo(required) > 0 ? required : total, price);
    }
}
