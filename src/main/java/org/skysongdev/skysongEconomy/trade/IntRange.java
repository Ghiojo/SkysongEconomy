package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.random.RandomGenerator;

/**
 * A whole number from {@code min} to {@code max}, both included; written {@code 3} or
 * {@code "1-3"} in ledger files.
 */
public record IntRange(int min, int max) {
    public IntRange {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Invalid range " + min + "-" + max);
        }
    }

    public static IntRange exactly(int value) {
        return new IntRange(value, value);
    }

    /**
     * Reads {@code key} as {@code 3} or {@code "1-3"}.
     *
     * @param fallback used when the key is missing, or {@code null} to require it
     * @throws ConfigException when it is missing, malformed, negative or reversed
     */
    public static IntRange parse(ConfigSection section, String key, IntRange fallback) {
        Object value = section.raw(key);
        if (value == null) {
            if (fallback == null) {
                throw section.error(key, "is required");
            }
            return fallback;
        }
        String text = value.toString().strip();
        try {
            int dash = text.indexOf('-', 1);
            IntRange range = dash < 0
                    ? exactly(Integer.parseInt(text))
                    : new IntRange(Integer.parseInt(text.substring(0, dash).strip()), Integer.parseInt(text.substring(dash + 1).strip()));
            return range;
        } catch (IllegalArgumentException e) {
            throw section.error(key, "must be a whole number like 3, or a range like \"1-3\" (got '" + text + "')");
        }
    }

    public boolean isExact() {
        return min == max;
    }

    /** @return a number from min to max, each equally likely */
    public int roll(RandomGenerator random) {
        return isExact() ? min : random.nextInt(min, max + 1);
    }

    /** @return the average roll */
    public BigDecimal average() {
        return BigDecimal.valueOf((long) min + max).divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
    }

    @Override
    public String toString() {
        return isExact() ? String.valueOf(min) : min + "-" + max;
    }
}
