package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootGroupLoader;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableLoader;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableParser;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableRegistry;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads the node files that {@code /se node reload} reloads: the loot groups in
 * {@code nodes/groups}, then the random tables in {@code nodes/random}, which roll on them.
 */
public final class NodeFiles {
    private final Path dataFolder;
    private final Predicate<String> itemExists;
    private final Predicate<String> skillExists;
    private final Consumer<String> warnings;

    /**
     * @param dataFolder  the plugin's data folder
     * @param itemExists  whether a material id has an item file
     * @param skillExists whether a skill id exists in SkysongStats
     * @param warnings    told about every file or entry that is skipped
     */
    public NodeFiles(Path dataFolder, Predicate<String> itemExists, Predicate<String> skillExists, Consumer<String> warnings) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.itemExists = itemExists;
        this.skillExists = skillExists;
        this.warnings = warnings;
    }

    /**
     * What one load read.
     *
     * @param groups   the loot groups that loaded
     * @param random   the random tables that loaded
     * @param warnings how many problems were found; each was logged
     */
    public record Loaded(LootGroupLibrary groups, RandomTableRegistry random, int warnings) {
    }

    /** Reads every file again. Broken files are skipped with a warning, so this never fails. */
    public Loaded load() {
        AtomicInteger count = new AtomicInteger();
        Consumer<String> counted = warning -> {
            count.incrementAndGet();
            warnings.accept(warning);
        };
        LootParser loot = new LootParser(itemExists);
        LootGroupLibrary groups = new LootGroupLoader(loot, counted).load(dataFolder.resolve(LootGroupLoader.FOLDER));
        RandomTableRegistry random = new RandomTableLoader(new RandomTableParser(loot, skillExists), counted)
                .load(dataFolder.resolve(RandomTableParser.FOLDER), groups);
        return new Loaded(groups, random, count.get());
    }
}
