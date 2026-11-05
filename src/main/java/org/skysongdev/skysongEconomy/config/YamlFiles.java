package org.skysongdev.skysongEconomy.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads the plugin's YAML config files from its data folder. Each feature has its own file; a
 * missing file is first copied from the defaults bundled in the jar.
 */
public final class YamlFiles {
    private final Path dataFolder;
    private final Consumer<String> saveDefault;

    /**
     * @param dataFolder  the plugin's data folder
     * @param saveDefault copies a bundled file to the data folder, e.g. {@code path -> plugin.saveResource(path, false)}
     */
    public YamlFiles(Path dataFolder, Consumer<String> saveDefault) {
        this.dataFolder = dataFolder;
        this.saveDefault = saveDefault;
    }

    /**
     * Loads one file, creating it from the bundled default first when it is missing.
     *
     * @param relativePath the file inside the data folder, with {@code /} separators, e.g. {@code businesses/types.yml}
     * @throws ConfigException when the file can't be read or isn't valid YAML
     */
    public ConfigSection load(String relativePath) {
        Path file = dataFolder.resolve(relativePath);
        if (Files.notExists(file)) {
            saveDefault.accept(relativePath);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(relativePath, reader);
        } catch (IOException e) {
            throw new ConfigException(relativePath + ": can't be read (" + e.getMessage() + ")");
        }
    }

    /**
     * Parses YAML text. Only plain maps, lists and scalars are allowed, never Java objects.
     *
     * @param name shown in error messages
     * @throws ConfigException when the text isn't valid YAML or its root isn't a section
     */
    public static ConfigSection parse(String name, Reader reader) {
        try {
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
            if (root != null && !(root instanceof Map<?, ?>)) {
                throw new ConfigException(name + ": the file must be a set of keys");
            }
            return ConfigSection.root(name, (Map<?, ?>) root);
        } catch (YAMLException e) {
            throw new ConfigException(name + ": invalid YAML (" + e.getMessage() + ")");
        }
    }
}
