package org.skysongdev.skysongEconomy.nodes.random;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFolder;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads every {@code <id>.yml} in the {@code nodes/random} folder, one table per file. A broken
 * file is skipped with a warning naming the file and key, and the other tables still load.
 */
public final class RandomTableLoader {
    private final RandomTableParser parser;
    private final Consumer<String> warnings;

    public RandomTableLoader(RandomTableParser parser, Consumer<String> warnings) {
        this.parser = parser;
        this.warnings = warnings;
    }

    /**
     * @param groups the loot groups the tables may roll on
     * @return the tables in {@code folder}; none when it doesn't exist
     */
    public RandomTableRegistry load(Path folder, LootGroupLibrary groups) {
        List<RandomTable> tables = new ArrayList<>();
        for (Map.Entry<String, ConfigSection> file : new YamlFolder(RandomTableParser.FOLDER, "random table", warnings)
                .read(folder).entrySet()) {
            try {
                tables.add(parser.parse(file.getKey(), file.getValue(), groups));
            } catch (ConfigException e) {
                warnings.accept("Skipped the random table '" + file.getKey() + "': " + e.getMessage());
            }
        }
        return new RandomTableRegistry(tables);
    }
}
