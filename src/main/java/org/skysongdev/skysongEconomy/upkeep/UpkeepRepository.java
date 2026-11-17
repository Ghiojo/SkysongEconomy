package org.skysongdev.skysongEconomy.upkeep;

import java.util.List;
import java.util.UUID;

/** Saves each NPC's upkeep. */
public interface UpkeepRepository {
    /** @return every saved upkeep */
    List<NpcUpkeep> loadAll();

    /** Saves an NPC's upkeep, replacing the one it had. */
    void save(NpcUpkeep upkeep);

    /** Forgets an NPC's upkeep. */
    void delete(UUID npc);
}
