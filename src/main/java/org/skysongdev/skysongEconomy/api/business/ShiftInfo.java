package org.skysongdev.skysongEconomy.api.business;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A shift at a business, as other plugins see it.
 *
 * @param id         unique
 * @param businessId where it is worked
 * @param character  who works it; the pay goes to their player
 * @param startedAt  when it started
 * @param required   how much online, non-AFK time it takes
 * @param worked     how much of that is done
 * @param payMoney   the money it pays the worker, fixed when it started
 * @param payTokens  the Gathering Tokens it pays the worker
 * @param vaultCut   the money it adds to the business's vault when it ends; 0 for the owner's own shifts
 * @param finishedAt when it ended; empty while it is being worked
 */
public record ShiftInfo(UUID id, String businessId, CharacterRef character, Instant startedAt, Duration required,
                        Duration worked, BigDecimal payMoney, long payTokens, BigDecimal vaultCut,
                        Optional<Instant> finishedAt) {
    public ShiftInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(worked, "worked");
        Objects.requireNonNull(payMoney, "payMoney");
        Objects.requireNonNull(vaultCut, "vaultCut");
        Objects.requireNonNull(finishedAt, "finishedAt");
    }
}
