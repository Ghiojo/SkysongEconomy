package org.skysongdev.skysongEconomy.journal;

import org.skysongdev.skysongEconomy.config.ConfigSection;

/**
 * What {@code journal.yml} says.
 *
 * @param enabled  whether the economy journal records anything; off, no records are kept and
 *                 {@code EconomyRecordedEvent} never fires
 * @param keepDays how many days records are kept, deleted when the plugin starts; 0 keeps them forever
 */
public record JournalSettings(boolean enabled, int keepDays) {
    /** The file, in the plugin's folder. */
    public static final String FILE = "journal.yml";
    /** Records forever, as the default file does. */
    public static final JournalSettings DEFAULT = new JournalSettings(true, 0);

    public JournalSettings {
        if (keepDays < 0) {
            throw new IllegalArgumentException("keep-days can't be negative: " + keepDays);
        }
    }

    /**
     * @throws org.skysongdev.skysongEconomy.config.ConfigException when {@code keep-days} is negative
     */
    public static JournalSettings parse(ConfigSection root) {
        int keepDays = root.integer("keep-days", 0);
        if (keepDays < 0) {
            throw root.error("keep-days", "must be 0 (forever) or more days, not " + keepDays);
        }
        return new JournalSettings(root.bool("enabled", true), keepDays);
    }
}
