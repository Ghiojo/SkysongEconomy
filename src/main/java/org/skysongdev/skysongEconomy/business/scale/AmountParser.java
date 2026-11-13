package org.skysongdev.skysongEconomy.business.scale;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.expression.Formula;
import org.skysongdev.skysongEconomy.expression.FormulaException;
import org.skysongdev.skysongEconomy.expression.Placeholders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Reads an {@link Amount} from a config key, in any of its three forms:
 * <pre>
 * amount: 4                              # a fixed number
 * amount: "3 + {level}"                  # a formula with {level} and {tier}
 * amount: {by: tier, 1: 1, 3: 2, 5: 3}   # a table; a row applies from its number upward
 * </pre>
 * A formula is checked here for every level and tier a business can have, so an amount that was
 * read never fails later.
 */
public final class AmountParser {
    private AmountParser() {
    }

    /**
     * @param section the section holding the key
     * @param key     the key to read
     * @return the amount, empty when the key is missing
     * @throws ConfigException when the value is none of the three forms, is negative, uses a
     *                         placeholder other than {@code {level}} and {@code {tier}}, or
     *                         divides by zero at some level and tier
     */
    public static Optional<Amount> read(ConfigSection section, String key) {
        Object raw = section.raw(key);
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof Map<?, ?>) {
            return Optional.of(table(section.section(key)));
        }
        if (raw instanceof Number number) {
            return Optional.of(fixed(section, key, number));
        }
        if (raw instanceof String text) {
            return Optional.of(computed(section, key, text));
        }
        throw section.error(key, "must be a number, a formula like \"3 + {level}\", or a table like {by: tier, 1: 1, 3: 2}");
    }

    /**
     * @return the amount at {@code key}, or {@code fallback} when the key is missing
     * @throws ConfigException as {@link #read}
     */
    public static Amount read(ConfigSection section, String key, Amount fallback) {
        return read(section, key).orElse(fallback);
    }

    /**
     * @return the amount at {@code key}
     * @throws ConfigException as {@link #read}, and when the key is missing
     */
    public static Amount require(ConfigSection section, String key) {
        return read(section, key).orElseThrow(() -> section.error(key, "is required"));
    }

    private static Amount fixed(ConfigSection section, String key, Number number) {
        BigDecimal value = new BigDecimal(number.toString());
        if (value.signum() < 0) {
            throw section.error(key, "must be 0 or more");
        }
        return new Amount.Fixed(Amount.whole(value));
    }

    private static Amount computed(ConfigSection section, String key, String text) {
        Formula formula;
        try {
            formula = Formula.parse(text, AmountParser::placeholder);
        } catch (FormulaException e) {
            throw section.error(key, "isn't a valid amount: '" + e.formula() + "' " + e.problem());
        }
        Amount amount = new Amount.Computed(formula);
        checkEverywhere(section, key, amount);
        return amount;
    }

    /** Works the formula out at every level and tier, so a division by zero shows up on load. */
    private static void checkEverywhere(ConfigSection section, String key, Amount amount) {
        for (int level = 0; level <= Business.MAX_TIER; level++) {
            for (int tier = Business.MIN_TIER; tier <= Business.MAX_TIER; tier++) {
                try {
                    amount.at(new BusinessScale(level, tier));
                } catch (FormulaException e) {
                    throw section.error(key, "isn't a valid amount: '" + e.formula() + "' " + e.problem()
                            + " at level " + level + ", tier " + tier);
                }
            }
        }
    }

    private static Amount table(ConfigSection table) {
        return new Amount.Table(StepsParser.parse(table, (rows, row) -> {
            Object raw = rows.raw(row);
            if (!(raw instanceof Number number) || new BigDecimal(number.toString()).signum() < 0) {
                throw rows.error(row, "must be a number, 0 or more");
            }
            return Amount.whole(new BigDecimal(number.toString()));
        }));
    }

    /** The placeholders an amount may use: {@code {level}} and {@code {tier}}. */
    private static String placeholder(String written) {
        return Basis.fromKey(written).map(Basis::key).orElseThrow(() ->
                new Placeholders.Unknown("has {" + written + "}: use {level} or {tier}"));
    }
}
