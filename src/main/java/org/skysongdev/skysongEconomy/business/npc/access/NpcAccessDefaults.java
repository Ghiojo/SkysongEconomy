package org.skysongdev.skysongEconomy.business.npc.access;

import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.config.ConfigException;
import org.skysongdev.skysongEconomy.config.ConfigSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What the built-in groups of a business may do with its NPCs until the owner changes it:
 * {@code default-access} in {@code businesses/npcs.yml}.
 * <pre>
 * default-access:
 *   workers: [info, stats-view, teleport, follow]
 *   everyone: []
 * </pre>
 * A value may also name a SkysongNPCs preset, such as {@code viewer}.
 *
 * @param byGroup the SkysongNPCs action names each built-in group starts with, by group id
 */
public record NpcAccessDefaults(Map<String, Set<String>> byGroup) {
    /** The key in {@code businesses/npcs.yml}. */
    public static final String KEY = "default-access";

    /** No group may do anything until the owner says so. */
    public static final NpcAccessDefaults NONE = new NpcAccessDefaults(Map.of());

    public NpcAccessDefaults {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        byGroup.forEach((group, actions) -> copy.put(group, Collections.unmodifiableSet(new LinkedHashSet<>(actions))));
        byGroup = Collections.unmodifiableMap(copy);
    }

    /**
     * @param root    the root of {@code businesses/npcs.yml}
     * @param actions knows SkysongNPCs' action names and presets
     * @throws ConfigException when a group is not {@code workers} or {@code everyone}, or a value
     *                         is neither an action nor a preset
     */
    public static NpcAccessDefaults parse(ConfigSection root, NpcActions actions) {
        if (!root.contains(KEY)) {
            return NONE;
        }
        ConfigSection section = root.section(KEY);
        Map<String, Set<String>> byGroup = new LinkedHashMap<>();
        for (String key : section.keys()) {
            String group = key.strip().toLowerCase(Locale.ROOT);
            if (!AccessGroup.isBuiltIn(group)) {
                throw section.error(key, "isn't a built-in group: only " + AccessGroup.WORKERS + " and "
                        + AccessGroup.EVERYONE + " have defaults");
            }
            Set<String> allowed = new LinkedHashSet<>();
            for (String written : section.strings(key)) {
                allowed.addAll(actions.resolve(written).orElseThrow(() -> section.error(key, "names '" + written
                        + "', which is neither a SkysongNPCs action nor a preset. Actions: "
                        + String.join(", ", actions.all()))));
            }
            byGroup.put(group, allowed);
        }
        return new NpcAccessDefaults(byGroup);
    }
}
