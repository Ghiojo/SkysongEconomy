package org.skysongdev.skysongEconomy.nodes.random;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Every random table that loaded, by id. */
public final class RandomTableRegistry {
    private final Map<String, RandomTable> tables;

    public RandomTableRegistry(Collection<RandomTable> tables) {
        Map<String, RandomTable> byId = new LinkedHashMap<>();
        tables.forEach(table -> byId.put(table.id(), table));
        this.tables = byId;
    }

    /** @return a registry with no tables */
    public static RandomTableRegistry empty() {
        return new RandomTableRegistry(List.of());
    }

    public Optional<RandomTable> find(String id) {
        return Optional.ofNullable(tables.get(id));
    }

    /** @return every table, in file name order */
    public Collection<RandomTable> all() {
        return List.copyOf(tables.values());
    }

    public int size() {
        return tables.size();
    }
}
