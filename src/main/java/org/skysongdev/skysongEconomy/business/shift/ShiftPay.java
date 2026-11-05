package org.skysongdev.skysongEconomy.business.shift;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * What one shift pays.
 *
 * @param money  paid into the player's balance
 * @param tokens Gathering Tokens for the character
 */
public record ShiftPay(BigDecimal money, long tokens) {
    /** Pays nothing. */
    public static final ShiftPay NONE = new ShiftPay(BigDecimal.ZERO, 0);

    public ShiftPay {
        Objects.requireNonNull(money, "money");
        if (money.signum() < 0 || tokens < 0) {
            throw new IllegalArgumentException("Pay must not be negative: " + money + " and " + tokens + " tokens");
        }
    }

    public ShiftPay plus(ShiftPay other) {
        return new ShiftPay(money.add(other.money), Math.addExact(tokens, other.tokens));
    }

    public boolean isNothing() {
        return money.signum() == 0 && tokens == 0;
    }
}
