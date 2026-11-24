package org.skysongdev.skysongEconomy.api.journal;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * The loss roll at an import's pier, on a collected import's record.
 *
 * @param lossChance  the pier's final loss chance, 0 to 100
 * @param roll        the number rolled, 0 (included) to 100 (excluded)
 * @param lostByRoll  whether the roll lost it, whatever listeners decided
 * @param seizeReason the text a listener gave for seizing it; empty when none did or it was delivered
 */
public record ImportRoll(BigDecimal lossChance, BigDecimal roll, boolean lostByRoll, Optional<String> seizeReason) {
    public ImportRoll {
        Objects.requireNonNull(lossChance, "lossChance");
        Objects.requireNonNull(roll, "roll");
        Objects.requireNonNull(seizeReason, "seizeReason");
    }
}
