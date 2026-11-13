package org.skysongdev.skysongEconomy.business.module.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Keeps the NPCs chosen for module benefits in a list, so tests can check what was written. */
public final class InMemoryModuleTargetRepository implements ModuleTargetRepository {
    public final List<Row> saved = new ArrayList<>();

    @Override
    public List<Row> loadAll() {
        return new ArrayList<>(saved);
    }

    @Override
    public void save(Row row) {
        saved.add(row);
    }

    @Override
    public void deletePick(String businessId, int pickOrder) {
        saved.removeIf(row -> row.businessId().equals(businessId) && row.pickOrder() == pickOrder);
    }

    @Override
    public void deleteNpc(UUID npc) {
        saved.removeIf(row -> row.npc().equals(npc));
    }
}
