package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.prices.PriceException;
import org.skysongdev.skysongEconomy.prices.PriceExpression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads one ledger file:
 * <pre>{@code
 * name: "<dark_gray>Smugglers' Ledger"
 * icon: ENDER_CHEST
 * description: ["<gray>Goods nobody asks questions about."]
 * imports:
 *   steel_weapons:
 *     name: "<white>Crate of steel weapons"
 *     icon: IRON_SWORD
 *     lore: ["<gray>Fresh from the forges."]
 *     contents:
 *       - {item: steel_sword, amount: 2}         # fixed
 *       - {item: steel_spear, amount: "1-3"}     # random amount
 *       - rolls: "1-2"                           # weighted rolls
 *         from:
 *           - {item: iron_dagger, weight: 3, amount: 1}
 *           - {item: steel_axe, weight: 1, amount: 1}
 *     prices:                                    # by delivery time; leave one out to not offer it
 *       express: "{base} * 1.5"
 *       regular: "{base}"
 * exports:
 *   steel_ingots:
 *     name: "<white>Steel ingots"
 *     icon: IRON_INGOT
 *     item: steel_ingot                          # or good-type: weapon
 *     batch: 16
 *     price: "{base} * 0.8"
 * }</pre>
 * A broken import or export is skipped with a warning so one typo doesn't hide a whole ledger.
 */
public final class LedgerParser {
    private final Set<String> deliveryTimes;
    private final Predicate<String> itemExists;
    private final Consumer<String> warnings;

    /**
     * @param deliveryTimes the ids of the server's delivery times
     * @param itemExists    whether a material id has an item file
     * @param warnings      told about each skipped entry
     */
    public LedgerParser(Set<String> deliveryTimes, Predicate<String> itemExists, Consumer<String> warnings) {
        this.deliveryTimes = Set.copyOf(deliveryTimes);
        this.itemExists = itemExists;
        this.warnings = warnings;
    }

    /**
     * @param id the ledger's id (its file name without {@code .yml})
     * @throws ConfigException when the ledger itself (its name or icon) is broken
     */
    public Ledger parse(String id, ConfigSection root) {
        Map<String, ImportOffer> imports = new LinkedHashMap<>();
        ConfigSection importsSection = root.section("imports");
        for (String key : importsSection.keys()) {
            try {
                imports.put(key, parseImport(key, importsSection.section(key)));
            } catch (ConfigException e) {
                warnings.accept("Skipped an import: " + e.getMessage());
            }
        }
        Map<String, ExportOffer> exports = new LinkedHashMap<>();
        ConfigSection exportsSection = root.section("exports");
        for (String key : exportsSection.keys()) {
            try {
                exports.put(key, parseExport(key, exportsSection.section(key)));
            } catch (ConfigException e) {
                warnings.accept("Skipped an export: " + e.getMessage());
            }
        }
        return new Ledger(id, root.requireString("name"), icon(root), root.strings("description"), imports, exports);
    }

    private ImportOffer parseImport(String id, ConfigSection section) {
        List<BundlePart> parts = new ArrayList<>();
        List<ConfigSection> contents = section.sections("contents");
        if (contents.isEmpty()) {
            throw section.error("contents", "must list at least one item");
        }
        for (ConfigSection part : contents) {
            parts.add(part.contains("rolls") ? rolls(part) : item(part));
        }
        Map<String, PriceExpression> prices = new LinkedHashMap<>();
        ConfigSection priceSection = section.section("prices");
        for (String time : priceSection.keys()) {
            if (!deliveryTimes.contains(time)) {
                warnings.accept(priceSection.error(time, "is not a delivery time in "
                        + EconomyLogisticsSettingsParser.FILE + ", so this price is ignored").getMessage());
                continue;
            }
            prices.put(time, expression(priceSection, time));
        }
        if (prices.isEmpty()) {
            throw section.error("prices", "must set a price for at least one delivery time");
        }
        return new ImportOffer(id, section.string("name", id), icon(section), section.strings("lore"), new Bundle(parts), prices);
    }

    private BundlePart item(ConfigSection part) {
        return new BundlePart.Item(materialId(part), IntRange.parse(part, "amount", IntRange.exactly(1)));
    }

    private BundlePart rolls(ConfigSection part) {
        List<BundlePart.Rolls.Option> options = new ArrayList<>();
        for (ConfigSection option : part.sections("from")) {
            int weight = option.integer("weight", 1);
            if (weight < 1) {
                throw option.error("weight", "must be 1 or more");
            }
            options.add(new BundlePart.Rolls.Option(materialId(option), weight,
                    IntRange.parse(option, "amount", IntRange.exactly(1))));
        }
        if (options.isEmpty()) {
            throw part.error("from", "must list at least one item to roll");
        }
        return new BundlePart.Rolls(IntRange.parse(part, "rolls", null), options);
    }

    private ExportOffer parseExport(String id, ConfigSection section) {
        ExportOffer.Target target;
        if (section.contains("item") == section.contains("good-type")) {
            throw section.error("item", "or good-type must be set, but not both");
        } else if (section.contains("item")) {
            target = new ExportOffer.Target.Item(materialId(section));
        } else {
            target = new ExportOffer.Target.GoodType(section.requireString("good-type").strip().toLowerCase(Locale.ROOT));
        }
        int batch = section.integer("batch", 1);
        if (batch < 1) {
            throw section.error("batch", "must be 1 or more");
        }
        return new ExportOffer(id, section.string("name", id), icon(section), section.strings("lore"), target, batch,
                expression(section, "price"));
    }

    private String materialId(ConfigSection section) {
        String id = section.requireString("item").strip().toLowerCase(Locale.ROOT);
        if (!itemExists.test(id)) {
            throw section.error("item", "'" + id + "' has no item file in the items folder");
        }
        return id;
    }

    private static PriceExpression expression(ConfigSection section, String key) {
        Object raw = section.raw(key);
        if (raw == null) {
            throw section.error(key, "is required");
        }
        try {
            return PriceExpression.parse(raw.toString());
        } catch (PriceException e) {
            throw section.error(key, "is not a valid price (" + raw + "): " + e.placeholders().getOrDefault("problem", e.messageKey()));
        }
    }

    private static String icon(ConfigSection section) {
        return section.string("icon", "PAPER").strip().toUpperCase(Locale.ROOT);
    }
}
