package org.skysongdev.skysongEconomy.nodes;

import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.config.YamlFolder;
import org.skysongdev.skysongEconomy.loot.LootGroupLibrary;
import org.skysongdev.skysongEconomy.loot.LootGroupLoader;
import org.skysongdev.skysongEconomy.loot.LootParser;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResource;
import org.skysongdev.skysongEconomy.nodes.farm.FarmResourceParser;
import org.skysongdev.skysongEconomy.nodes.farm.FarmType;
import org.skysongdev.skysongEconomy.nodes.farm.FarmTypeParser;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarModifier;
import org.skysongdev.skysongEconomy.nodes.modifiers.CalendarModifierParser;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableParser;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads the node files that {@code /se node reload} reloads, in the order they depend on each
 * other: the loot groups in {@code nodes/groups}, the random tables in {@code nodes/random} and
 * the farm resources in {@code nodes/farm/resources}, which roll on the groups, then the farm
 * types in {@code nodes/farm/types}, which bundle the resources, and last the calendar modifiers in
 * {@code nodes/modifiers}, which name all of them. Modifiers are only read with SkysongCalendar,
 * whose season and month names they are checked against.
 */
public final class NodeFiles {
    private final Path dataFolder;
    private final Predicate<String> itemExists;
    private final Predicate<String> skillExists;
    private final Optional<CalendarNames> calendar;
    private final Consumer<String> warnings;

    /** Same as {@link #NodeFiles(Path, Predicate, Predicate, Optional, Consumer)} without a calendar. */
    public NodeFiles(Path dataFolder, Predicate<String> itemExists, Predicate<String> skillExists, Consumer<String> warnings) {
        this(dataFolder, itemExists, skillExists, Optional.empty(), warnings);
    }

    /**
     * @param dataFolder  the plugin's data folder
     * @param itemExists  whether a material id has an item file
     * @param skillExists whether a skill id exists in SkysongStats
     * @param calendar    SkysongCalendar's seasons and months; empty without SkysongCalendar, and
     *                    then no calendar modifier is read
     * @param warnings    told about every file or entry that is skipped
     */
    public NodeFiles(Path dataFolder, Predicate<String> itemExists, Predicate<String> skillExists,
                     Optional<CalendarNames> calendar, Consumer<String> warnings) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.itemExists = itemExists;
        this.skillExists = skillExists;
        this.calendar = Objects.requireNonNull(calendar, "calendar");
        this.warnings = warnings;
    }

    /**
     * What one load read.
     *
     * @param groups        the loot groups that loaded
     * @param random        the random tables that loaded
     * @param farmResources the farm resources that loaded
     * @param farmTypes     the farm types that loaded
     * @param modifiers     the calendar modifiers that loaded; none without SkysongCalendar
     * @param warnings      how many problems were found; each was logged
     */
    public record Loaded(LootGroupLibrary groups, FileRegistry<RandomTable> random,
                         FileRegistry<FarmResource> farmResources, FileRegistry<FarmType> farmTypes,
                         FileRegistry<CalendarModifier> modifiers, int warnings) {
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
        FileRegistry<CalendarModifier> modifiers = loadModifiers(random, resources, types, counted);
        return new Loaded(groups, random, resources, types, modifiers, count.get());
    }

    private FileRegistry<CalendarModifier> loadModifiers(FileRegistry<RandomTable> random,
                                                         FileRegistry<FarmResource> resources,
                                                         FileRegistry<FarmType> types, Consumer<String> counted) {
        YamlFolder folder = new YamlFolder(CalendarModifierParser.FOLDER, "calendar modifier", counted);
        Path path = dataFolder.resolve(CalendarModifierParser.FOLDER);
        if (calendar.isEmpty()) {
            int files = folder.read(path).size();
            if (files > 0) {
                counted.accept("SkysongCalendar is not installed, so the " + files + " file(s) in "
                        + CalendarModifierParser.FOLDER + " are not loaded and no calendar modifier applies");
            }
            return FileRegistry.empty();
        }
        Set<String> randomTypes = random.all().stream().map(RandomTable::type).collect(Collectors.toSet());
        CalendarModifierParser parser = new CalendarModifierParser(calendar.get(),
                new CalendarModifierParser.KnownSources(random.ids(), randomTypes, types.ids(), resources.ids()), counted);
        return folder.parseEach(path, parser::parse);
    }
}
