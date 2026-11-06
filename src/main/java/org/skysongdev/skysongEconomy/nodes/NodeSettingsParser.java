package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.business.display.NameplateSettingsParser;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads {@code nodes/settings.yml}:
 * <pre>{@code
 * owner-business-types: [gathering]
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

    private final Predicate<String> businessTypeExists;
    private final Consumer<String> warnings;

    /**
     * @param businessTypeExists whether a business type id is in {@code businesses/types.yml}
     * @param warnings           told about unknown business types, which are left out
     */
    public NodeSettingsParser(Predicate<String> businessTypeExists, Consumer<String> warnings) {
        this.businessTypeExists = businessTypeExists;
        this.warnings = warnings;
    }

    /**
     * @throws ConfigException when a value is the wrong type or out of range
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
        return new NodeSettings(types, new NameplateSettingsParser().parse(root));
    }
}
