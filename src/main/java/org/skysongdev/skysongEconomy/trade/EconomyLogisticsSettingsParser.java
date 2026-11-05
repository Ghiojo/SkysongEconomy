package org.skysongdev.skysongEconomy.trade;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Reads {@code economy-and-logistics.yml}:
 * <pre>{@code
 * skills: [economical]
 * slots:              # by how many Economy and Logistics picks the business has
 *   1: {imports: 1, ledgers: 1}
 *   2: {imports: 2, ledgers: 1}
 * delivery-times:
 *   express: {name: "<gold>Express", hours: 24}
 * ledger-removal-fine: 50
 * }</pre>
 */
public final class EconomyLogisticsSettingsParser {
    /** File name used in error messages and when loading. */
    public static final String FILE = "economy-and-logistics.yml";

    /** How many delivery times the Imports window has room for. */
    public static final int MAX_SHOWN_DELIVERY_TIMES = 5;

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private final SkillCatalog skills;
    private final Consumer<String> warnings;

    /**
     * @param skills   used to warn about skills that don't exist
     * @param warnings told about each problem that doesn't stop loading
     */
    public EconomyLogisticsSettingsParser(SkillCatalog skills, Consumer<String> warnings) {
        this.skills = skills;
        this.warnings = warnings;
    }

    /**
     * @throws ConfigException when a value is missing or out of range
     */
    public EconomyLogisticsSettings parse(ConfigSection root) {
        return new EconomyLogisticsSettings(skills(root), slots(root), deliveryTimes(root), fine(root));
    }

    private Set<String> skills(ConfigSection root) {
        List<String> listed = root.contains("skills") ? root.strings("skills") : List.of("economical");
        Set<String> result = new LinkedHashSet<>();
        for (String skill : listed) {
            String id = skill.strip().toLowerCase(Locale.ROOT);
            if (!skills.exists(id)) {
                warnings.accept(FILE + ": the skill '" + id + "' doesn't exist in SkysongStats, so its picks never count");
            }
            result.add(id);
        }
        if (result.isEmpty()) {
            throw root.error("skills", "must list at least one skill");
        }
        return result;
    }

    private static Map<Integer, SlotLimits> slots(ConfigSection root) {
        ConfigSection slots = root.section("slots");
        Map<Integer, SlotLimits> byLevel = new HashMap<>();
        for (String key : slots.keys()) {
            int level = level(slots, key);
            ConfigSection row = slots.section(key);
            int imports = row.integer("imports", 0);
            int ledgers = row.integer("ledgers", 0);
            if (imports < 0) {
                throw row.error("imports", "must be 0 or more");
            }
            if (ledgers < 0) {
                throw row.error("ledgers", "must be 0 or more");
            }
            byLevel.put(level, new SlotLimits(imports, ledgers));
        }
        if (byLevel.isEmpty()) {
            throw root.error("slots", "must set the limits of at least level 1");
        }
        return byLevel;
    }

    private static int level(ConfigSection slots, String key) {
        try {
            int level = Integer.parseInt(key);
            if (level >= 1 && level <= Business.MAX_TIER) {
                return level;
            }
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw slots.error(key, "is not a level: use 1 to " + Business.MAX_TIER);
    }

    private Map<String, DeliveryTime> deliveryTimes(ConfigSection root) {
        ConfigSection times = root.section("delivery-times");
        Map<String, DeliveryTime> byId = new LinkedHashMap<>();
        for (String id : times.keys()) {
            if (!ID.matcher(id).matches()) {
                throw times.error(id, "is not a valid id: use lowercase letters, digits, - and _");
            }
            ConfigSection row = times.section(id);
            int hours = row.integer("hours", 0);
            if (hours < 1) {
                throw row.error("hours", "must be 1 or more");
            }
            byId.put(id, new DeliveryTime(id, row.string("name", id), Duration.ofHours(hours)));
        }
        if (byId.isEmpty()) {
            throw root.error("delivery-times", "must define at least one delivery time");
        }
        if (byId.size() > MAX_SHOWN_DELIVERY_TIMES) {
            warnings.accept(FILE + ": only the first " + MAX_SHOWN_DELIVERY_TIMES
                    + " delivery times fit in the Imports window; the others can't be chosen");
        }
        return byId;
    }

    private static BigDecimal fine(ConfigSection root) {
        double fine = root.number("ledger-removal-fine", 50);
        if (fine < 0) {
            throw root.error("ledger-removal-fine", "must be 0 or more");
        }
        return BigDecimal.valueOf(fine);
    }
}
