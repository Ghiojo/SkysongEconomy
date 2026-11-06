package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.config.YamlFolder;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootGroupLoader;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResource;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResourceParser;
import org.skysongdev.skysongEconomy.nodes.farm.FarmType;
import org.skysongdev.skysongEconomy.nodes.farm.FarmTypeParser;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableParser;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads the node files that {@code /se node reload} reloads, in the order they depend on each
 * other: the loot groups in {@code nodes/groups}, the random tables in {@code nodes/random} and
 * the farm resources in {@code nodes/farm/resources}, which roll on the groups, then the farm
 * types in {@code nodes/farm/types}, which bundle the resources.
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
     * @param groups        the loot groups that loaded
     * @param random        the random tables that loaded
     * @param farmResources the farm resources that loaded
     * @param farmTypes     the farm types that loaded
     * @param warnings      how many problems were found; each was logged
     */
    public record Loaded(LootGroupLibrary groups, FileRegistry<RandomTable> random,
                         FileRegistry<FarmResource> farmResources, FileRegistry<FarmType> farmTypes, int warnings) {
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

        RandomTableParser randomParser = new RandomTableParser(loot, skillExists);
        FileRegistry<RandomTable> random = new YamlFolder(RandomTableParser.FOLDER, "random table", counted)
                .parseEach(dataFolder.resolve(RandomTableParser.FOLDER), (id, root) -> randomParser.parse(id, root, groups));

        FarmResourceParser resourceParser = new FarmResourceParser(loot, itemExists);
        FileRegistry<FarmResource> resources = new YamlFolder(FarmResourceParser.FOLDER, "farm resource", counted)
                .parseEach(dataFolder.resolve(FarmResourceParser.FOLDER), (id, root) -> resourceParser.parse(id, root, groups));

        FarmTypeParser typeParser = new FarmTypeParser(skillExists, counted);
        FileRegistry<FarmType> types = new YamlFolder(FarmTypeParser.FOLDER, "farm type", counted)
                .parseEach(dataFolder.resolve(FarmTypeParser.FOLDER), (id, root) -> typeParser.parse(id, root, resources));
        return new Loaded(groups, random, resources, types, count.get());
    }
}
