package org.skysongdev.skysongEconomy.business.bench;

import org.skysongdev.skysongEconomy.config.ConfigSection;

/**
 * Reads {@code businesses/benches.yml}:
 * <pre>{@code
 * business-staff-can-use: true
 * }</pre>
 */
public final class BenchSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "businesses/benches.yml";

    public BenchSettings parse(ConfigSection root) {
        return new BenchSettings(root.bool("business-staff-can-use", true));
    }
}
