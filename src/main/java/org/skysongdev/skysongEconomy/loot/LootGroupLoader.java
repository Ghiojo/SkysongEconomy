package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads every {@code <id>.yml} in the {@code nodes/groups} folder, one loot group per file. A file
 * that can't be read or is broken is skipped with a warning.
 */
public final class LootGroupLoader {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "nodes/groups";

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private final LootParser parser;
    private final Consumer<String> warnings;

    public LootGroupLoader(LootParser parser, Consumer<String> warnings) {
        this.parser = parser;
        this.warnings = warnings;
    }

    /** @return the groups in {@code folder}; none when it doesn't exist */
    public LootGroupLibrary load(Path folder) {
        if (!Files.isDirectory(folder)) {
            return LootGroupLibrary.empty();
        }
        List<Path> paths;
        try (Stream<Path> listing = Files.list(folder)) {
            paths = listing.filter(path -> path.getFileName().toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            warnings.accept("Couldn't list the " + FOLDER + " folder: " + e.getMessage());
            return LootGroupLibrary.empty();
        }
        Map<String, ConfigSection> files = new LinkedHashMap<>();
        for (Path path : paths) {
            String name = path.getFileName().toString();
            String id = name.substring(0, name.length() - ".yml".length()).toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                warnings.accept("Skipped the loot group file " + name + ": use lowercase letters, digits, - and _ in its name");
                continue;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                files.put(id, YamlFiles.parse(FOLDER + "/" + name, reader));
            } catch (IOException | ConfigException e) {
                warnings.accept("Skipped the loot group file " + name + ": " + e.getMessage());
            }
        }
        return parser.parseGroups(files, warnings);
    }
}
