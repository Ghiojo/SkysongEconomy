package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.config.YamlFolder;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Reads every {@code <id>.yml} in the {@code nodes/groups} folder, one loot group per file. A file
 * that can't be read or is broken is skipped with a warning.
 */
public final class LootGroupLoader {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "nodes/groups";

    private final LootParser parser;
    private final Consumer<String> warnings;

    public LootGroupLoader(LootParser parser, Consumer<String> warnings) {
        this.parser = parser;
        this.warnings = warnings;
    }

    /** @return the groups in {@code folder}; none when it doesn't exist */
    public LootGroupLibrary load(Path folder) {
        return parser.parseGroups(new YamlFolder(FOLDER, "loot group", warnings).read(folder), warnings);
    }
}
