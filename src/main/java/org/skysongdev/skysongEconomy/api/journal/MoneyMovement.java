package org.skysongdev.skysongEconomy.api.journal;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Money that moved from one account to another.
 *
 * @param from   where it came from
 * @param to     where it went
 * @param amount how much, more than 0
 */
public record MoneyMovement(MoneyAccount from, MoneyAccount to, BigDecimal amount) {
    public MoneyMovement {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A movement moves more than 0: " + amount);
        }
    }
}
