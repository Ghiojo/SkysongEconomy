package org.skysongdev.skysongEconomy.upkeep;

import org.skysongdev.skysongEconomy.items.StoredItems;

import java.util.Map;
import java.util.UUID;

/** Saves the items stocked in each NPC for its upkeep. */
public interface UpkeepStockRepository {
    /** @return every NPC's stocked items */
    Map<UUID, StoredItems> loadAll();

    /** Saves an NPC's stocked items, replacing what it had. */
    void save(UUID npc, StoredItems items);

    /** Forgets an NPC's stocked items. */
    void delete(UUID npc);
}
