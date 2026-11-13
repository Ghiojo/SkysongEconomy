package org.skysongdev.skysongEconomy.business.module;

import java.util.ArrayList;
import java.util.List;

/** Keeps module picks in a list, so tests can check what was written. */
public final class InMemoryModulePickRepository implements ModulePickRepository {
    public final List<ModulePick> saved = new ArrayList<>();

    @Override
    public List<ModulePick> loadAll() {
        return new ArrayList<>(saved);
    }

    @Override
    public void save(ModulePick pick) {
        saved.add(pick);
    }

    @Override
    public void delete(String businessId, int order) {
        saved.removeIf(pick -> pick.businessId().equals(businessId) && pick.order() == order);
    }
}
