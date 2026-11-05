package org.skysongdev.skysongEconomy.loot;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.trade.IntRange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads loot group files and outputs.
 * <p>
 * A group file ({@code nodes/groups/<id>.yml}) lists weighted entries. Each entry has a whole
 * {@code weight} (default 1) and exactly one of {@code item}, {@code group}, {@code from} or
 * {@code nothing}:
 * <pre>{@code
 * entries:
 *   - {item: copper, weight: 10, amount: "1-3"}   # an item, amount fixed or "min-max" (default 1)
 *   - {group: rare-pebbles, weight: 1}            # roll once inside nodes/groups/rare-pebbles.yml
 *   - weight: 2                                   # roll once inside a list written here
 *     from:
 *       - {item: tin}
 *       - {item: coal, weight: 3, amount: 2}
 *   - {nothing: true, weight: 5}                  # a wasted roll
 * }</pre>
 * An output, used by every tier of a node table. Each list is optional, as long as one is set:
 * <pre>{@code
 * guaranteed:
 *   - {item: wheat, amount: "2-4"}
 * chances:                                        # each rolled on its own, in percent
 *   - {item: golden_seed, chance: 2.5, amount: "1-2"}
 * rolls:
 *   - times: "6-8"                                # default 1
 *     from:                                       # the same entries as a group file
 *       - {group: unskilled-ores, weight: 60}
 *       - {item: wheat, weight: 10, amount: "1-3"}
 *   - {times: 2, group: novice-ores}              # or name a group file
 * }</pre>
 * A group that points back to itself, directly or through other groups, is refused.
 */
public final class LootParser {
    private final Predicate<String> itemExists;

    /** @param itemExists whether a material id has an item file */
    public LootParser(Predicate<String> itemExists) {
        this.itemExists = itemExists;
    }

    /**
     * Reads every group file. A broken group is left out with a warning, and so is every group
     * that points to it, so one typo doesn't hide the rest.
     *
     * @param files    each group file's root section, keyed by group id
     * @param warnings told about each group left out
     * @return the groups that loaded
     */
    public LootGroupLibrary parseGroups(Map<String, ConfigSection> files, Consumer<String> warnings) {
        GroupBuilder builder = new GroupBuilder(files, LootGroupLibrary.empty());
        Map<String, LootGroup> built = new LinkedHashMap<>();
        for (String id : files.keySet()) {
            try {
                built.put(id, builder.named(id, files.get(id)));
            } catch (ConfigException e) {
                warnings.accept("Skipped the loot group '" + id + "': " + e.getMessage());
            }
        }
        return new LootGroupLibrary(built);
    }

    /**
     * Reads an output.
     *
     * @param groups the groups its rolls may name
     * @throws ConfigException when an entry is broken or it gives nothing at all
     */
    public LootOutput parseOutput(ConfigSection section, LootGroupLibrary groups) {
        List<LootEntry.Item> guaranteed = new ArrayList<>();
        for (ConfigSection item : section.sections("guaranteed")) {
            guaranteed.add(item(item));
        }
        List<ChanceItem> chances = new ArrayList<>();
        for (ConfigSection chance : section.sections("chances")) {
            chances.add(chance(chance));
        }
        GroupBuilder builder = new GroupBuilder(Map.of(), groups);
        List<GroupRolls> rolls = new ArrayList<>();
        for (ConfigSection roll : section.sections("rolls")) {
            rolls.add(new GroupRolls(IntRange.parse(roll, "times", IntRange.exactly(1)), builder.target(roll)));
        }
        if (guaranteed.isEmpty() && chances.isEmpty() && rolls.isEmpty()) {
            throw section.error("guaranteed", "chances or rolls must give at least one item");
        }
        return new LootOutput(guaranteed, chances, rolls);
    }

    private LootEntry.Item item(ConfigSection section) {
        String id = section.requireString("item").strip().toLowerCase(Locale.ROOT);
        if (!itemExists.test(id)) {
            throw section.error("item", "'" + id + "' has no item file in the items folder");
        }
        return new LootEntry.Item(id, IntRange.parse(section, "amount", IntRange.exactly(1)));
    }

    private ChanceItem chance(ConfigSection section) {
        if (!section.contains("chance")) {
            throw section.error("chance", "is required (a percent, like 12.5)");
        }
        double percent = section.number("chance", 0);
        if (!(percent > 0 && percent <= 100)) {
            throw section.error("chance", "must be above 0 and at most 100 (got " + percent + ")");
        }
        return new ChanceItem(item(section), percent);
    }

    /**
     * Builds groups on demand, so a group file can point to any other file, whatever the load
     * order, and loops are caught.
     */
    private final class GroupBuilder {
        private final Map<String, ConfigSection> files;
        private final LootGroupLibrary loaded;
        private final Map<String, LootGroup> built = new LinkedHashMap<>();
        private final Set<String> building = new LinkedHashSet<>();

        /**
         * @param files  group files that may still need building
         * @param loaded groups that are already built
         */
        GroupBuilder(Map<String, ConfigSection> files, LootGroupLibrary loaded) {
            this.files = files;
            this.loaded = loaded;
        }

        LootGroup named(String id, ConfigSection file) {
            LootGroup done = built.get(id);
            if (done != null) {
                return done;
            }
            if (!building.add(id)) {
                throw file.error("entries", "loops back to itself: " + String.join(" -> ", building) + " -> " + id);
            }
            try {
                LootGroup group = build(id, file, "entries");
                built.put(id, group);
                return group;
            } finally {
                building.remove(id);
            }
        }

        /** The group an entry or a roll points to, through {@code group} or {@code from}. */
        LootGroup target(ConfigSection entry) {
            boolean named = entry.contains("group");
            if (named == entry.contains("from")) {
                throw entry.error("group", "or from must be set, but not both");
            }
            if (!named) {
                return build(entry.path(), entry, "from");
            }
            String id = entry.requireString("group").strip().toLowerCase(Locale.ROOT);
            ConfigSection file = files.get(id);
            if (file != null) {
                return named(id, file);
            }
            return loaded.find(id).orElseThrow(() -> entry.error("group", "'" + id + "' is not a loaded group in nodes/groups"));
        }

        /**
         * @param id    the group's id, or its path in the file when it is written inline
         * @param owner the section holding the list at {@code key}
         */
        private LootGroup build(String id, ConfigSection owner, String key) {
            List<ConfigSection> lines = owner.sections(key);
            if (lines.isEmpty()) {
                throw owner.error(key, "must list at least one entry");
            }
            List<WeightedEntry> entries = new ArrayList<>(lines.size());
            for (ConfigSection line : lines) {
                int weight = line.integer("weight", 1);
                if (weight < 1) {
                    throw line.error("weight", "must be a whole number of 1 or more");
                }
                entries.add(new WeightedEntry(entry(line), weight));
            }
            return new LootGroup(id, entries);
        }

        private LootEntry entry(ConfigSection line) {
            int kinds = (line.contains("item") ? 1 : 0) + (line.contains("group") ? 1 : 0)
                    + (line.contains("from") ? 1 : 0) + (line.bool("nothing", false) ? 1 : 0);
            if (kinds != 1) {
                throw line.error("item", "group, from or nothing: true must be set, and only one of them");
            }
            if (line.contains("item")) {
                return item(line);
            }
            if (line.contains("group") || line.contains("from")) {
                return new LootEntry.Group(target(line));
            }
            return new LootEntry.Nothing();
        }
    }
}
