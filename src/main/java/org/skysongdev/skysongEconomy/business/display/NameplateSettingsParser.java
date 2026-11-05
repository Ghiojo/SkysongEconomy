package org.skysongdev.skysongEconomy.business.display;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

/**
 * Reads the {@code nameplate} section of {@code businesses/block.yml}:
 * <pre>{@code
 * nameplate:
 *   enabled: true
 *   height: 0.4
 *   view-distance: 16
 *   shadow: true
 *   background: false
 * }</pre>
 * The text itself is {@code business.nameplate} in {@code messages.yml}.
 */
public final class NameplateSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/block.yml";

    /**
     * @throws ConfigException when a value is out of range
     */
    public NameplateSettings parse(ConfigSection root) {
        ConfigSection section = root.section("nameplate");
        double height = section.number("height", 0.4);
        if (height < 0) {
            throw section.error("height", "must be 0 or more");
        }
        int viewDistance = section.integer("view-distance", 16);
        if (viewDistance < 1) {
            throw section.error("view-distance", "must be at least 1");
        }
        return new NameplateSettings(section.bool("enabled", true), height, viewDistance,
                section.bool("shadow", true), section.bool("background", false));
    }
}
