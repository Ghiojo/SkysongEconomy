package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads every {@code <id>.yml} in the {@code ledgers} folder. A file that can't be read, or whose
 * name or icon is broken, is skipped with a warning.
 */
public final class LedgerLoader {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private final LedgerParser parser;
    private final Consumer<String> warnings;

    public LedgerLoader(LedgerParser parser, Consumer<String> warnings) {
        this.parser = parser;
        this.warnings = warnings;
    }

    /** @return the ledgers in {@code folder}, sorted by file name; none when it doesn't exist */
    public LedgerRegistry load(Path folder) {
        List<Ledger> ledgers = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return new LedgerRegistry(ledgers);
        }
        List<Path> files;
        try (Stream<Path> listing = Files.list(folder)) {
            files = listing.filter(path -> path.getFileName().toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            warnings.accept("Couldn't list the ledgers folder: " + e.getMessage());
            return new LedgerRegistry(ledgers);
        }
        for (Path file : files) {
            String name = file.getFileName().toString();
            String id = name.substring(0, name.length() - ".yml".length()).toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                warnings.accept("Skipped the ledger file " + name + ": use lowercase letters, digits, - and _ in its name");
                continue;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                ledgers.add(parser.parse(id, YamlFiles.parse(folder.getFileName() + "/" + name, reader)));
            } catch (IOException | ConfigException e) {
                warnings.accept("Skipped the ledger file " + name + ": " + e.getMessage());
            }
        }
        return new LedgerRegistry(ledgers);
    }
}
