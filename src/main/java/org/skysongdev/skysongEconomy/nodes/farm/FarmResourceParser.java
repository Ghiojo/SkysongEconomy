package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.nodes.TierKeys;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reads one farm resource file:
 * <pre>{@code
 * name: "<white>Sheep"                      # what players see; default the file name
 * primer: {item: sheep_token, amount: 2}    # what planting takes; amount default 1
 * tiers:                                    # only the tiers listed exist, from 1 to 5
 *   3:
 *     cost: 3                               # Harvest Tokens per harvest
 *     guaranteed: [{item: wool, amount: "2-3"}]
 *     chances: [{item: mutton, chance: 25, amount: "1-2"}]
 * }</pre>
 * Each tier's output is read by {@link LootParser#parseOutput}, the same as a random table's.
 */
public final class FarmResourceParser {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "nodes/farm/resources";

    private final LootParser loot;
    private final Predicate<String> itemExists;

    /**
     * @param loot       reads each tier's output
     * @param itemExists whether a material id has an item file, for the primer
     */
    public FarmResourceParser(LootParser loot, Predicate<String> itemExists) {
        this.loot = loot;
        this.itemExists = itemExists;
    }

    /**
     * @param id     the file name without {@code .yml}
     * @param groups the loot groups the tiers may roll on
     * @throws ConfigException when anything in the file is missing or wrong
     */
    public FarmResource parse(String id, ConfigSection root, LootGroupLibrary groups) {
        ItemAmount primer = primer(root);
        Map<Integer, FarmTier> tiers = TierKeys.readTiers(root,
                (number, section) -> new FarmTier(number, TierKeys.cost(section), loot.parseOutput(section, groups)));
        return new FarmResource(id, root.string("name", id), primer, tiers);
    }

    private ItemAmount primer(ConfigSection root) {
        if (!root.contains("primer")) {
            throw root.error("primer", "is required, like {item: wheat_seeds, amount: 1}");
        }
        ConfigSection primer = root.section("primer");
        String item = primer.requireString("item").strip().toLowerCase(Locale.ROOT);
        if (!itemExists.test(item)) {
            throw primer.error("item", "'" + item + "' has no item file in the items folder");
        }
        int amount = primer.integer("amount", 1);
        if (amount < 1) {
            throw primer.error("amount", "must be 1 or more");
        }
        return new ItemAmount(item, amount);
    }
}
