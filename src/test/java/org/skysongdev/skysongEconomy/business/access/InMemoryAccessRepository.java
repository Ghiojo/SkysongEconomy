package org.skysongdev.skysongEconomy.business.access;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps saved groups in a map, so tests can check what was written. */
public final class InMemoryAccessRepository implements AccessRepository {
    final Map<String, List<AccessGroup>> saved = new LinkedHashMap<>();
    int writes;

    @Override
    public Map<String, List<AccessGroup>> loadAll() {
        return new LinkedHashMap<>(saved);
    }

    @Override
    public void save(String businessId, List<AccessGroup> groups) {
        saved.put(businessId, List.copyOf(groups));
        writes++;
    }
}
