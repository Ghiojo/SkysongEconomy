package org.skysongdev.skysongEconomy.business.npc.benefit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps the sheet changes in a map, so tests can check what was written. */
public final class InMemoryNpcAppliedRepository implements NpcAppliedRepository {
    public final Map<UUID, NpcApplied> saved = new LinkedHashMap<>();

    @Override
    public Map<UUID, NpcApplied> loadAll() {
        return new LinkedHashMap<>(saved);
    }

    @Override
    public void save(UUID npc, NpcApplied applied) {
        if (applied.isEmpty()) {
            saved.remove(npc);
        } else {
            saved.put(npc, applied);
        }
    }
}
