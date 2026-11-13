package org.skysongdev.skysongEconomy.business.npc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps business NPCs in a map, so tests can check what was written. */
public final class InMemoryBusinessNpcRepository implements BusinessNpcRepository {
    public final Map<UUID, BusinessNpc> saved = new LinkedHashMap<>();

    @Override
    public List<BusinessNpc> loadAll() {
        return new ArrayList<>(saved.values());
    }

    @Override
    public void save(BusinessNpc npc) {
        saved.put(npc.npc(), npc);
    }

    @Override
    public void delete(UUID npc) {
        saved.remove(npc);
    }
}
