package org.skysongdev.skysongEconomy.prices;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Settings from {@code prices.yml}: how many decimals prices keep. Prices are truncated, never
 * rounded, so with 1 decimal 4.09 becomes 4.0.
 *
 * @param decimals 0 to 4
 */
public record PriceSettings(int decimals) {
    /** File name used in error messages and when loading. */
    public static final String FILE = "prices.yml";
    /** The most decimals the database stores. */
    public static final int MAX_DECIMALS = 4;

    public PriceSettings {
        if (decimals < 0 || decimals > MAX_DECIMALS) {
            throw new IllegalArgumentException("decimals must be 0 to " + MAX_DECIMALS + ": " + decimals);
        }
    }

    /**
     * @throws ConfigException when decimals is out of range
     */
    public static PriceSettings parse(ConfigSection root) {
        int decimals = root.integer("decimals", 1);
        if (decimals < 0 || decimals > MAX_DECIMALS) {
            throw root.error("decimals", "must be 0 to " + MAX_DECIMALS);
        }
        return new PriceSettings(decimals);
    }

    /** @return the price cut to the configured decimals, towards zero */
    public BigDecimal truncate(BigDecimal price) {
        return price.setScale(decimals, RoundingMode.DOWN);
    }
}
