package org.skysongdev.skysongEconomy.messaging;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Writes numbers for players: odds and averages with up to two decimals, no trailing zeros. */
public final class NumberText {
    private static final int DECIMALS = 2;

    private NumberText() {
    }

    /** @return a chance from 0 to 1 as a percent, such as "12.5"; "<0.01" when it rounds to 0 but isn't */
    public static String percent(double chance) {
        String text = decimal(chance * 100);
        return chance > 0 && text.equals("0") ? "<0.01" : text;
    }

    /** @return the number with up to two decimals, such as "2.33" or "4" */
    public static String decimal(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
