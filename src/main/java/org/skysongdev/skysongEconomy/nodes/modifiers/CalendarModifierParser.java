package org.skysongdev.skysongEconomy.nodes.modifiers;

import org.skysongdev.skysongEconomy.calendar.CalendarNames;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;
import org.skysongdev.skysongEconomy.loot.YieldEffect;
import org.skysongdev.skysongEconomy.nodes.Node;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reads one calendar modifier file:
 * <pre>{@code
 * name: "<aqua>Winter frost"          # what players see; default the file name
 * when:                               # on when ANY of these holds today's IC date
 *   seasons: [vinse_er]               # SkysongCalendar's seasons, by id or name
 *   dates:                            # month days, every year; a range may wrap past the new year
 *     - {from: "Rana 10", to: "Litoria 45"}
 *     - {day: "Celestine 1"}
 *   festival: true                    # the festival days that close the year
 * applies-to:                         # a node is hit when ANY line names it
 *   random-tables: [herbs]
 *   random-types: [foraging]
 *   farm-types: [crop]
 *   farm-resources: [wheat]
 *   nodes: [north_field]
 *   tiers: [1, 2]                     # optional: only these tiers
 * effect: "max-50%; rolls-1; chance-50%"   # or: dormant
 * }</pre>
 * Season and month names are matched against the installed SkysongCalendar's, never a list of
 * SkysongEconomy's own, and an unknown one names the ones that exist. Tables, types and resources
 * that didn't load only warn, so a modifier can name one that is added later.
 */
public final class CalendarModifierParser {
    /** The folder, relative to the plugin's data folder. */
    public static final String FOLDER = "nodes/modifiers";
    /** The effect that stops harvests instead of changing them. */
    public static final String DORMANT = "dormant";

    private final CalendarNames calendar;
    private final KnownSources known;
    private final Consumer<String> warnings;

    /**
     * @param calendar the installed SkysongCalendar's seasons and months
     * @param known    the tables, types and resources that loaded, to warn about the others
     * @param warnings told about names in {@code applies-to} that didn't load
     */
    public CalendarModifierParser(CalendarNames calendar, KnownSources known, Consumer<String> warnings) {
        this.calendar = Objects.requireNonNull(calendar, "calendar");
        this.known = Objects.requireNonNull(known, "known");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /**
     * The ids that loaded, by kind.
     *
     * @param randomTables  random table ids
     * @param randomTypes   the random tables' booster types
     * @param farmTypes     farm type ids
     * @param farmResources farm resource ids
     */
    public record KnownSources(Set<String> randomTables, Set<String> randomTypes, Set<String> farmTypes,
                               Set<String> farmResources) {
        public KnownSources {
            randomTables = Set.copyOf(randomTables);
            randomTypes = Set.copyOf(randomTypes);
            farmTypes = Set.copyOf(farmTypes);
            farmResources = Set.copyOf(farmResources);
        }
    }

    /**
     * @param id the file name without {@code .yml}
     * @throws ConfigException when anything in the file is missing or wrong
     */
    public CalendarModifier parse(String id, ConfigSection root) {
        CalendarWindow window = window(root.section("when"));
        NodeTarget target = target(id, root.section("applies-to"));
        String effectText = root.requireString("effect").strip();
        boolean dormant = effectText.equalsIgnoreCase(DORMANT);
        YieldEffect effect = YieldEffect.NONE;
        if (!dormant) {
            try {
                effect = YieldEffect.parse(effectText);
            } catch (IllegalArgumentException e) {
                throw root.error("effect", e.getMessage() + ", or write " + DORMANT + " to stop harvests");
            }
        }
        return new CalendarModifier(id, root.string("name", id), window, target, effect, dormant);
    }

    private CalendarWindow window(ConfigSection when) {
        Set<String> seasons = new LinkedHashSet<>();
        for (String written : when.strings("seasons")) {
            seasons.add(calendar.season(written).orElseThrow(() -> when.error("seasons",
                    "has no season '" + written.strip() + "'; the seasons are " + calendar.seasonList())).id());
        }
        List<CalendarWindow.DateRange> dates = new ArrayList<>();
        for (ConfigSection range : when.sections("dates")) {
            dates.add(range(range));
        }
        boolean festival = when.bool("festival", false);
        if (seasons.isEmpty() && dates.isEmpty() && !festival) {
            throw when.error("seasons", "dates or festival must say when the modifier is on");
        }
        return new CalendarWindow(seasons, dates, festival);
    }

    private CalendarWindow.DateRange range(ConfigSection range) {
        if (range.contains("day")) {
            int day = dayOfYear(range, "day");
            return new CalendarWindow.DateRange(day, day);
        }
        if (!range.contains("from") || !range.contains("to")) {
            throw range.error("from", "and to are both needed, like {from: \"Rana 10\", to: \"Litoria 45\"},"
                    + " or day for one day, like {day: \"Celestine 1\"}");
        }
        return new CalendarWindow.DateRange(dayOfYear(range, "from"), dayOfYear(range, "to"));
    }

    private int dayOfYear(ConfigSection range, String key) {
        try {
            CalendarNames.MonthDay day = calendar.monthDay(range.requireString(key));
            return calendar.dayOfYear(day.month(), day.day());
        } catch (IllegalArgumentException e) {
            throw range.error(key, e.getMessage());
        }
    }

    private NodeTarget target(String id, ConfigSection appliesTo) {
        Set<String> tables = ids(id, appliesTo, "random-tables", known.randomTables());
        Set<String> types = ids(id, appliesTo, "random-types", known.randomTypes());
        Set<String> farmTypes = ids(id, appliesTo, "farm-types", known.farmTypes());
        Set<String> resources = ids(id, appliesTo, "farm-resources", known.farmResources());
        Set<String> nodes = ids(id, appliesTo, "nodes", null);
        if (tables.isEmpty() && types.isEmpty() && farmTypes.isEmpty() && resources.isEmpty() && nodes.isEmpty()) {
            throw appliesTo.error("random-tables", "random-types, farm-types, farm-resources or nodes must name what"
                    + " the modifier hits");
        }
        Set<Integer> tiers = new LinkedHashSet<>();
        for (String tier : appliesTo.strings("tiers")) {
            tiers.add(tier(appliesTo, tier));
        }
        return new NodeTarget(tables, types, farmTypes, resources, nodes, tiers);
    }

    /** @param loaded the ids that loaded, {@code null} when they can't be checked here (nodes live in the database) */
    private Set<String> ids(String id, ConfigSection appliesTo, String key, Set<String> loaded) {
        Set<String> ids = new LinkedHashSet<>();
        for (String written : appliesTo.strings(key)) {
            String lower = written.strip().toLowerCase(Locale.ROOT);
            if (loaded != null && !loaded.contains(lower)) {
                warnings.accept("The modifier '" + id + "' names '" + lower + "' in applies-to." + key
                        + ", which is missing or didn't load; it applies once that file loads");
            }
            ids.add(lower);
        }
        return ids;
    }

    private static int tier(ConfigSection appliesTo, String written) {
        try {
            int tier = Integer.parseInt(written.strip());
            if (tier >= Node.MIN_TIER && tier <= Node.MAX_TIER) {
                return tier;
            }
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw appliesTo.error("tiers", "must list tiers from " + Node.MIN_TIER + " to " + Node.MAX_TIER
                + " (got '" + written.strip() + "')");
    }
}
