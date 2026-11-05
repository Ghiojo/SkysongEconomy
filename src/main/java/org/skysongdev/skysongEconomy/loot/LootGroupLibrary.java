package org.skysongdev.skysongEconomy.loot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Named groups that tables can roll on and that other groups can point to. */
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

    /** @return the group named {@code id} */
    public Optional<LootGroup> find(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    /** @return the names of every group, in file order */
    public Set<String> ids() {
        return groups.keySet();
    }

    /**
     * @return a library with this one's groups and {@code other}'s; where both have a name, this
     *         one's group wins, so a table's own group can shadow a shared one
     */
    public LootGroupLibrary with(LootGroupLibrary other) {
        Map<String, LootGroup> merged = new LinkedHashMap<>(other.groups);
        merged.putAll(groups);
        return new LootGroupLibrary(merged);
    }
}
