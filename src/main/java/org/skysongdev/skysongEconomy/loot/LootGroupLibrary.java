package org.skysongdev.skysongEconomy.loot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The groups loaded from the {@code nodes/groups} folder, by id (file name without {@code .yml}). */
public final class LootGroupLibrary {
    private static final LootGroupLibrary EMPTY = new LootGroupLibrary(Map.of());

    private final Map<String, LootGroup> groups;

    LootGroupLibrary(Map<String, LootGroup> groups) {
        this.groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
    }

    /** @return a library with no groups */
    public static LootGroupLibrary empty() {
        return EMPTY;
    }

    /** @return the group with this id */
    public Optional<LootGroup> find(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    /** @return the id of every group, in load order */
    public Set<String> ids() {
        return groups.keySet();
    }
}
