package org.skysongdev.skysongEconomy.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps saved businesses in a map, so tests can check what was written. */
final class InMemoryBusinessRepository implements BusinessRepository {
    final Map<String, BusinessSnapshot> saved = new LinkedHashMap<>();
    int writes;

    @Override
    public List<BusinessSnapshot> loadAll() {
        return new ArrayList<>(saved.values());
    }

    @Override
    public void save(BusinessSnapshot business) {
        saved.put(business.id(), business);
        writes++;
    }

    @Override
    public void delete(String id) {
        saved.remove(id);
        writes++;
    }
}
