package org.skysongdev.skysongEconomy.api.business;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A training session at a business, as other plugins see it.
 *
 * @param id         unique
 * @param businessId where it is trained
 * @param character  who trains
 * @param startedAt  when it started
 * @param required   how much online, non-AFK time it takes
 * @param price      what the character paid for it; it went to the business's vault
 */
public record TrainingInfo(UUID id, String businessId, CharacterRef character, Instant startedAt, Duration required,
                           BigDecimal price) {
    public TrainingInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(price, "price");
    }
}
