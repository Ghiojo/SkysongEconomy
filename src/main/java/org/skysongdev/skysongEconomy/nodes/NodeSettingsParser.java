package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.business.display.NameplateSettingsParser;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads {@code nodes/settings.yml}:
 * <pre>{@code
 * owner-business-types: [gathering]
 * tier-skill-levels:          # tiers left out keep these defaults
 *   1: unskilled
 *   2: novice
 *   3: competent
 *   4: journeyman
 *   5: expert
 * nameplate:
 *   enabled: true
 *   height: 0.4
 *   view-distance: 16
 *   shadow: true
 *   background: false
 * }</pre>
 * The nameplate text itself is {@code node.nameplate} in {@code messages.yml}.
 */
public final class NodeSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "nodes/settings.yml";
    private static final String TIER_LEVELS = "tier-skill-levels";

    private final Predicate<String> businessTypeExists;
    private final Predicate<String> levelExists;
    private final Consumer<String> warnings;

    /**
     * @param businessTypeExists whether a business type id is in {@code businesses/types.yml}
     * @param levelExists        whether a skill level id exists in SkysongStats
     * @param warnings           told about unknown business types, which are left out
     */
    public NodeSettingsParser(Predicate<String> businessTypeExists, Predicate<String> levelExists, Consumer<String> warnings) {
        this.businessTypeExists = businessTypeExists;
        this.levelExists = levelExists;
        this.warnings = warnings;
    }

    /**
     * @throws ConfigException when a value is the wrong type or out of range, or a skill level is unknown
     */
    public NodeSettings parse(ConfigSection root) {
        Set<String> types = new LinkedHashSet<>();
        for (String type : root.strings("owner-business-types")) {
            String id = type.strip().toLowerCase(Locale.ROOT);
            if (businessTypeExists.test(id)) {
                types.add(id);
            } else {
                warnings.accept(root.error("owner-business-types", "lists '" + id
                        + "', which isn't a business type, so it is ignored").getMessage());
            }
        }
        return new NodeSettings(types, tierLevels(root.section(TIER_LEVELS)), new NameplateSettingsParser().parse(root));
    }

    private Map<Integer, String> tierLevels(ConfigSection section) {
        Map<Integer, String> levels = new HashMap<>();
        for (int tier = Node.MIN_TIER; tier <= Node.MAX_TIER; tier++) {
            levels.put(tier, NodeSettings.DEFAULT_TIER_LEVELS.get(tier - Node.MIN_TIER));
        }
        for (String key : section.keys()) {
            int tier = TierKeys.parse(section, key);
            String level = section.requireString(key).strip().toLowerCase(Locale.ROOT);
            if (!levelExists.test(level)) {
                throw section.error(key, "'" + level + "' isn't a skill level in SkysongStats");
            }
            levels.put(tier, level);
        }
        return levels;
    }
}
