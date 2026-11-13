package org.skysongdev.skysongEconomy.business.scale;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Reads a {@link Steps} table from a config section:
 * <pre>
 * by: level        # or tier; level when left out
 * 1: proficient
 * 4: adept
 * </pre>
 * Every key but {@code by} is a row's number, from 1 to the highest business tier.
 */
public final class StepsParser {
    /** The key that says which number the rows follow. */
    public static final String BY = "by";

    private StepsParser() {
    }

    /**
     * @param table the section holding {@code by} and the rows
     * @param value reads one row's value from the section, given the row's key
     * @param <T>   what each row holds
     * @return the table
     * @throws ConfigException when {@code by} isn't level or tier, a row's key isn't a number
     *                         from 1 to 5, there is no row, or a value can't be read
     */
    public static <T> Steps<T> parse(ConfigSection table, BiFunction<ConfigSection, String, T> value) {
        Basis basis = basis(table);
        Map<Integer, T> rows = new LinkedHashMap<>();
        for (String key : table.keys()) {
            if (key.equals(BY)) {
                continue;
            }
            rows.put(rowNumber(table, key), value.apply(table, key));
        }
        if (rows.isEmpty()) {
            throw new ConfigException(table.path() + " needs at least one row, like 1: ...");
        }
        return new Steps<>(basis, rows);
    }

    private static Basis basis(ConfigSection table) {
        String written = table.string(BY, Basis.LEVEL.key());
        return Basis.fromKey(written).orElseThrow(() -> table.error(BY, "must be level or tier, not '" + written + "'"));
    }

    private static int rowNumber(ConfigSection table, String key) {
        int number;
        try {
            number = Integer.parseInt(key.strip());
        } catch (NumberFormatException e) {
            throw table.error(key, "isn't a row: rows are numbers from " + Business.MIN_TIER + " to " + Business.MAX_TIER);
        }
        if (number < Business.MIN_TIER || number > Business.MAX_TIER) {
            throw table.error(key, "isn't a row: rows are numbers from " + Business.MIN_TIER + " to " + Business.MAX_TIER);
        }
        return number;
    }
}
