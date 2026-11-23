package org.skysongdev.skysongEconomy.trade.pier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps saved piers in maps, so tests can look at what would be in the database. */
final class InMemoryPierRepository implements PierRepository {
    final Map<String, Row> piers = new LinkedHashMap<>();
    /** By pier id + ":" + modifier id. */
    final Map<String, ModifierRow> modifiers = new LinkedHashMap<>();

    @Override
    public List<Row> loadPiers() {
        return new ArrayList<>(piers.values());
    }

    @Override
    public List<ModifierRow> loadModifiers() {
        return new ArrayList<>(modifiers.values());
    }

    @Override
    public void savePier(Row row) {
        piers.put(row.id(), row);
    }

    @Override
    public void deletePier(String id) {
        piers.remove(id);
        modifiers.keySet().removeIf(key -> key.startsWith(id + ":"));
    }

    @Override
    public void saveModifier(ModifierRow row) {
        modifiers.put(row.pierId() + ":" + row.modifier().id(), row);
    }

    @Override
    public void deleteModifier(String pierId, String modifierId) {
        modifiers.remove(pierId + ":" + modifierId);
    }
}
