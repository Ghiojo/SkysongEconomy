package org.skysongdev.skysongEconomy.config;

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
 * Reads a folder where each {@code <id>.yml} file is one thing, named by its file name, such as
 * the loot groups in {@code nodes/groups} or the tables in {@code nodes/random}. A file that can't
 * be read, or whose name isn't a valid id, is skipped with a warning, so the others still load.
 */
public final class YamlFolder {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");
    private static final String EXTENSION = ".yml";

    private final String folderName;
    private final String what;
    private final Consumer<String> warnings;

    /**
     * @param folderName the folder relative to the plugin's data folder, used in messages and
     *                   in the files' names for errors
     * @param what       what one file holds, such as {@code "loot group"}, used in warnings
     * @param warnings   told about every file that is skipped
     */
    public YamlFolder(String folderName, String what, Consumer<String> warnings) {
        this.folderName = folderName;
        this.what = what;
        this.warnings = warnings;
    }

    /** @return each file's root section, keyed by its id in file name order; none when the folder is missing */
    public Map<String, ConfigSection> read(Path folder) {
        Map<String, ConfigSection> files = new LinkedHashMap<>();
        if (!Files.isDirectory(folder)) {
            return files;
        }
        List<Path> paths;
        try (Stream<Path> listing = Files.list(folder)) {
            paths = listing.filter(path -> path.getFileName().toString().endsWith(EXTENSION)).sorted().toList();
        } catch (IOException e) {
            warnings.accept("Couldn't list the " + folderName + " folder: " + e.getMessage());
            return files;
        }
        for (Path path : paths) {
            String name = path.getFileName().toString();
            String id = name.substring(0, name.length() - EXTENSION.length()).toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                warnings.accept("Skipped the " + what + " file " + name
                        + ": use lowercase letters, digits, - and _ in its name");
                continue;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                files.put(id, YamlFiles.parse(folderName + "/" + name, reader));
            } catch (IOException | ConfigException e) {
                warnings.accept("Skipped the " + what + " file " + name + ": " + e.getMessage());
            }
        }
        return files;
    }
}
