package org.skysongdev.skysongEconomy.trade.pier;

import org.skysongdev.skysongEconomy.api.piers.LossModifier;
import org.skysongdev.skysongEconomy.api.piers.PierDetails;
import org.skysongdev.skysongEconomy.api.piers.PierException;
import org.skysongdev.skysongEconomy.api.piers.Piers;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reads {@code piers.yml}:
 * <pre>{@code
 * piers:
 *   harbour:
 *     name: "<aqua>Harbour Pier"
 *     icon: OAK_BOAT
 *     description: ["<gray>The main docks."]
 *     data: {faction: crown}             # optional, for other plugins
 *     loss-chance: 2                     # optional, percent, default 0
 *     loss-modifiers:                    # optional
 *       rough_waters: "+3"               # points for every shipment
 *       patrols: {amount: "+10%", tag: contraband}
 * }</pre>
 * A broken pier, or a broken modifier, is skipped with a warning so one typo doesn't hide the rest.
 */
public final class PierSettingsParser {
    /** The file, relative to the plugin folder. */
    public static final String FILE = "piers.yml";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Consumer<String> warnings;

    /** @param warnings told about each skipped pier or modifier */
    public PierSettingsParser(Consumer<String> warnings) {
        this.warnings = warnings;
    }

    public PierSettings parse(ConfigSection root) {
        Map<String, ConfiguredPier> piers = new LinkedHashMap<>();
        ConfigSection section = root.section("piers");
        for (String key : section.keys()) {
            try {
                ConfiguredPier pier = pier(key, section.section(key));
                if (piers.putIfAbsent(pier.id(), pier) != null) {
                    warnings.accept(section.error(key, "is the same id as another pier, so it is skipped").getMessage());
                }
            } catch (ConfigException e) {
                warnings.accept("Skipped a pier: " + e.getMessage());
            }
        }
        return new PierSettings(piers);
    }

    private ConfiguredPier pier(String key, ConfigSection section) {
        String id;
        try {
            id = Piers.checkId(key, "pier id");
        } catch (PierException e) {
            throw new ConfigException(section.path() + " must be an id of letters, digits, _ and -");
        }
        PierDetails details = new PierDetails(section.string("name", id), section.string("icon", "PAPER"),
                section.strings("description"));
        return new ConfiguredPier(id, details, data(section.section("data")), chance(section),
                modifiers(section.section("loss-modifiers")));
    }

    private static Map<String, String> data(ConfigSection data) {
        Map<String, String> values = new LinkedHashMap<>();
        data.leaves().forEach((key, value) -> values.put(key, value.toString()));
        return values;
    }

    private static BigDecimal chance(ConfigSection section) {
        Object raw = section.raw("loss-chance");
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal chance = number(section, "loss-chance", raw.toString().replace("%", "").strip());
        if (chance.signum() < 0 || chance.compareTo(HUNDRED) > 0) {
            throw section.error("loss-chance", "must be from 0 to 100");
        }
        return chance;
    }

    private Map<String, LossModifier> modifiers(ConfigSection section) {
        Map<String, LossModifier> modifiers = new LinkedHashMap<>();
        for (String key : section.keys()) {
            try {
                LossModifier modifier = modifier(section, key);
                modifiers.put(modifier.id(), modifier);
            } catch (ConfigException e) {
                warnings.accept("Skipped a loss modifier: " + e.getMessage());
            }
        }
        return modifiers;
    }

    private static LossModifier modifier(ConfigSection section, String key) {
        String amount;
        Optional<String> tag = Optional.empty();
        if (section.raw(key) instanceof Map<?, ?>) {
            ConfigSection details = section.section(key);
            amount = details.requireString("amount");
            tag = Optional.ofNullable(details.string("tag", null));
        } else {
            amount = section.requireString(key);
        }
        try {
            return LossModifier.parse(key, amount, tag);
        } catch (NumberFormatException e) {
            throw section.error(key, "must be an amount such as +5 or +10%, not '" + amount + "'");
        } catch (PierException e) {
            throw section.error(key, "must be an id of letters, digits, _ and -");
        }
    }

    private static BigDecimal number(ConfigSection section, String key, String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw section.error(key, "must be a number, not '" + text + "'");
        }
    }
}
