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
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Reads loot groups and outputs from config sections.
 * <p>
 * Groups, by name. Each entry has a {@code weight} (a whole number, default 1) and exactly one
 * of {@code item}, {@code group}, {@code from} or {@code nothing}:
 * <pre>{@code
 * groups:
 *   common:
 *     - {item: copper_ore, weight: 6, amount: "1-3"}   # an item, amount fixed or "min-max"
 *     - {group: gems, weight: 1}                       # roll inside another named group
 *     - weight: 2                                      # roll inside a group written here
 *       from:
 *         - {item: tin_ore, weight: 1}
 *         - {item: coal, weight: 3, amount: 2}
 *     - {nothing: true, weight: 1}                     # a wasted roll
 * }</pre>
 * An output, which every list is optional in, as long as one is set:
 * <pre>{@code
 * guaranteed:
 *   - {item: wheat, amount: "2-4"}
 * chances:                                             # each one rolled on its own
 *   - {item: golden_seed, chance: 2.5, amount: 1}      # chance in percent
 * rolls:
 *   - {times: "6-8", group: common}                    # or from: [...] like a group entry
 * }</pre>
 * A group that points back to itself, directly or through other groups, is refused.
 */
public final class LootParser {
    private static final Pattern GROUP_ID = Pattern.compile("[a-z0-9_-]+");

    private final Predicate<String> itemExists;

    /** @param itemExists whether a material id has an item file */
    public LootParser(Predicate<String> itemExists) {
        this.itemExists = itemExists;
    }

    /**
     * Reads every group in {@code section}, keyed by name.
     *
     * @param shared groups the entries may also point to, such as the shared groups folder;
     *               a group in {@code section} with the same name is used instead
     * @return the groups of {@code section} only
     * @throws ConfigException when a group is empty, broken, unknown or part of a loop
     */
    public LootGroupLibrary parseGroups(ConfigSection section, LootGroupLibrary shared) {
        Map<String, List<ConfigSection>> definitions = new LinkedHashMap<>();
        for (String id : section.keys()) {
            if (!GROUP_ID.matcher(id).matches()) {
                throw section.error(id, "is not a valid group name: use lowercase letters, digits, - and _");
            }
            definitions.put(id, section.sections(id));
        }
        GroupBuilder builder = new GroupBuilder(section, definitions, shared);
        Map<String, LootGroup> built = new LinkedHashMap<>();
        for (String id : definitions.keySet()) {
            built.put(id, builder.named(id));
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
        GroupBuilder builder = new GroupBuilder(section, Map.of(), groups);
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

    /** Builds named groups on demand, so a group can point to one written further down. */
    private final class GroupBuilder {
        private final ConfigSection section;
        private final Map<String, List<ConfigSection>> definitions;
        private final LootGroupLibrary shared;
        private final Map<String, LootGroup> built = new LinkedHashMap<>();
        private final Set<String> building = new LinkedHashSet<>();

        GroupBuilder(ConfigSection section, Map<String, List<ConfigSection>> definitions, LootGroupLibrary shared) {
            this.section = section;
            this.definitions = definitions;
            this.shared = shared;
        }

        LootGroup named(String id) {
            LootGroup done = built.get(id);
            if (done != null) {
                return done;
            }
            if (!building.add(id)) {
                throw section.error(id, "loops back to itself: " + String.join(" -> ", building) + " -> " + id);
            }
            LootGroup group = build(id, definitions.get(id), section, id);
            building.remove(id);
            built.put(id, group);
            return group;
        }

        /** The group an entry or a roll points to, through {@code group} or {@code from}. */
        LootGroup target(ConfigSection entry) {
            boolean named = entry.contains("group");
            if (named == entry.contains("from")) {
                throw entry.error("group", "or from must be set, but not both");
            }
            if (!named) {
                return build(entry.path(), entry.sections("from"), entry, "from");
            }
            String id = entry.requireString("group").strip().toLowerCase(Locale.ROOT);
            if (definitions.containsKey(id)) {
                return named(id);
            }
            return shared.find(id).orElseThrow(() -> entry.error("group", "'" + id + "' is not a group"));
        }

        /**
         * @param id    the group's name, or its path in the file when it is written inline
         * @param owner the section holding {@code key}, named in errors
         */
        private LootGroup build(String id, List<ConfigSection> lines, ConfigSection owner, String key) {
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
