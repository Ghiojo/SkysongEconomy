package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.trade.GoodTypes;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Every item's good types, kept in memory so prices can be worked out without a database query,
 * and written through to the {@link GoodTypeRepository}. {@code /se good} changes them through
 * this class so the copy stays current.
 */
public final class CachedGoodTypes implements GoodTypes {
    private final GoodTypeRepository repository;
    private final Map<String, Set<String>> byItem = new HashMap<>();

    public CachedGoodTypes(GoodTypeRepository repository) {
        this.repository = repository;
    }

    /** Reads every good type from the database. */
    public void loadAll() throws SQLException {
        byItem.clear();
        repository.loadAll().forEach((item, types) -> byItem.put(item, new LinkedHashSet<>(types)));
    }

    @Override
    public Set<String> of(String materialId) {
        return Set.copyOf(byItem.getOrDefault(materialId, Set.of()));
    }

    /** @return false if the item already had this good type */
    public boolean add(String materialId, String goodType) throws SQLException {
        boolean added = repository.add(materialId, goodType);
        byItem.computeIfAbsent(materialId, id -> new LinkedHashSet<>()).add(goodType);
        return added;
    }

    /** @return whether the item had this good type */
    public boolean remove(String materialId, String goodType) throws SQLException {
        boolean removed = repository.remove(materialId, goodType);
        Set<String> types = byItem.get(materialId);
        if (types != null) {
            types.remove(goodType);
        }
        return removed;
    }
}
