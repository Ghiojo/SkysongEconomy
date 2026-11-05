package org.skysongdev.skysongEconomy.config;

import java.nio.file.Path;

/** Where the plugin keeps its files inside its data folder. */
public record PluginPaths(Path dataFolder) {
    /** Folder in the jar and in the data folder that holds the Architite coin item files. */
    public static final String COINS_FOLDER = "architite_coins";

    /** Skysong item files ({@code <materialId>.yml}) that import offers deliver. */
    public Path items() {
        return dataFolder.resolve("items");
    }

    /** One sub-folder per import catalogue, each holding one {@code .yml} file per offer. */
    public Path imports() {
        return dataFolder.resolve("imports");
    }

    public Path coins() {
        return dataFolder.resolve(COINS_FOLDER);
    }
}
