package org.skysongdev.skysongEconomy.upkeep;

import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reads {@code npc-upkeep.yml}:
 * <pre>
 * check-every: 60                      # seconds between settling cycles that are due
 * name-line-height: 0.2                # blocks above an unmaintained NPC's head for its red line
 * profiles:
 *   guard_rations:
 *     every: 24h                       # one cycle, real time: s, m, h, d, w, such as 1d12h
 *     slots: 18                        # stock size: 9, 18, 27, 36, 45 or 54
 *     cost:                            # paid in this order, all or nothing
 *       - {good-type: food, amount: 3}
 *       - {item: water_flask, amount: 1}
 * templates:
 *   business_guard: guard_rations      # SkysongNPCs template: profile
 * </pre>
 * A broken profile or template line stops the file from loading, so no NPC follows half a
 * profile. Items and item categories that nothing has yet only warn, as they may be added later.
 */
public final class UpkeepSettingsParser {
    /** The file, inside the plugin's data folder. */
    public static final String FILE = "npc-upkeep.yml";

    private final Predicate<String> itemExists;
    private final Predicate<String> goodTypeExists;
    private final Consumer<String> warnings;

    /**
     * @param itemExists     whether a SkysongItems {@code material_id} exists
     * @param goodTypeExists whether any item has that item category
     * @param warnings       told about ids that don't exist yet
     */
    public UpkeepSettingsParser(Predicate<String> itemExists, Predicate<String> goodTypeExists, Consumer<String> warnings) {
        this.itemExists = Objects.requireNonNull(itemExists, "itemExists");
        this.goodTypeExists = Objects.requireNonNull(goodTypeExists, "goodTypeExists");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
    }

    /** @throws ConfigException naming the key at fault */
    public UpkeepSettings parse(ConfigSection root) {
        int checkSeconds = root.integer("check-every", (int) UpkeepSettings.DEFAULT_CHECK_EVERY.toSeconds());
        if (checkSeconds < 1) {
            throw root.error("check-every", "must be 1 second or more");
        }
        ConfigSection profileSection = root.section("profiles");
        Map<String, UpkeepProfile> profiles = new LinkedHashMap<>();
        for (String id : profileSection.keys()) {
            profiles.put(id, profile(id, profileSection.section(id)));
        }
        ConfigSection templateSection = root.section("templates");
        Map<String, String> templates = new LinkedHashMap<>();
        for (String template : templateSection.keys()) {
            String profile = templateSection.requireString(template).strip();
            if (!profiles.containsKey(profile)) {
                throw templateSection.error(template, "names the profile '" + profile + "', which is not under profiles");
            }
            templates.put(template.toLowerCase(Locale.ROOT), profile);
        }
        double nameLineHeight = root.number("name-line-height", UpkeepSettings.DEFAULT_NAME_LINE_HEIGHT);
        if (nameLineHeight < -2 || nameLineHeight > 2) {
            throw root.error("name-line-height", "must be between -2 and 2 blocks");
        }
        return new UpkeepSettings(Duration.ofSeconds(checkSeconds), profiles, templates, nameLineHeight);
    }

    private UpkeepProfile profile(String id, ConfigSection section) {
        Duration every;
        try {
            every = CycleLength.parse(section.requireString("every"));
        } catch (IllegalArgumentException e) {
            throw section.error("every", "is not a length of time such as 24h, 3d or 1d12h");
        }
        int slots = section.integer("slots", 9);
        if (!UpkeepProfile.SLOT_SIZES.contains(slots)) {
            throw section.error("slots", "must be one of " + UpkeepProfile.SLOT_SIZES);
        }
        List<ConfigSection> rows = section.sections("cost");
        if (rows.isEmpty()) {
            throw section.error("cost", "needs at least one entry, such as {item: bread, amount: 3}");
        }
        List<UpkeepCost> cost = new ArrayList<>(rows.size());
        for (ConfigSection row : rows) {
            cost.add(entry(row));
        }
        return new UpkeepProfile(id, every, slots, cost);
    }

    private UpkeepCost entry(ConfigSection row) {
        if (row.contains("item") == row.contains("good-type")) {
            throw row.error("item", "or good-type must be set, but not both");
        }
        int amount = row.integer("amount", 1);
        if (amount < 1) {
            throw row.error("amount", "must be 1 or more");
        }
        UpkeepCost.Match match;
        if (row.contains("item")) {
            String item = row.requireString("item").strip();
            if (!itemExists.test(item)) {
                warnings.accept(row.path() + " item: there is no item '" + item + "' yet");
            }
            match = new UpkeepCost.Match.Item(item);
        } else {
            String type = row.requireString("good-type").strip().toLowerCase(Locale.ROOT);
            if (!goodTypeExists.test(type)) {
                warnings.accept(row.path() + " good-type: no item has the item category '" + type + "' yet");
            }
            match = new UpkeepCost.Match.GoodType(type);
        }
        return new UpkeepCost(match, amount);
    }
}
