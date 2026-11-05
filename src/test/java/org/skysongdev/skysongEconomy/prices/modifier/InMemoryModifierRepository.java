package org.skysongdev.skysongEconomy.prices.modifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps modifiers in a map, for tests. */
final class InMemoryModifierRepository implements ModifierRepository {
    final Map<String, PriceModifier> saved = new LinkedHashMap<>();

    @Override
    public List<PriceModifier> loadAll() {
        return new ArrayList<>(saved.values());
    }

    @Override
    public void save(PriceModifier modifier) {
        saved.put(modifier.id(), modifier);
    }

    @Override
    public void delete(String id) {
        saved.remove(id);
    }
}
