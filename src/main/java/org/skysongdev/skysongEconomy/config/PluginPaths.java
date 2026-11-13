package org.skysongdev.skysongEconomy.config;

import java.nio.file.Path;

/** Where the plugin keeps its files inside its data folder. */
public record PluginPaths(Path dataFolder) {
    /** Folder in the jar and in the data folder that holds the Architite coin item files. */
    public static final String COINS_FOLDER = "architite_coins";
    /** Folder in the jar and in the data folder that holds the trade ledgers. */
    public static final String LEDGERS_FOLDER = "ledgers";

    /** Folder in the jar and in the data folder that holds the business modules. */
    public static final String MODULES_FOLDER = "modules";

    /** Skysong item files ({@code <materialId>.yml}) that imports deliver and exports buy. */
    public Path items() {
        return dataFolder.resolve("items");
    }

    /** One {@code <ledgerId>.yml} file per trade ledger. */
    public Path ledgers() {
        return dataFolder.resolve(LEDGERS_FOLDER);
    }

    /** One {@code <moduleId>.yml} file per business module. */
    public Path modules() {
        return dataFolder.resolve(MODULES_FOLDER);
    }

    public Path coins() {
        return dataFolder.resolve(COINS_FOLDER);
    }
}
