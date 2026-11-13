package org.skysongdev.skysongEconomy.business.npc;

import java.util.List;
import java.util.UUID;

/** Where it is saved which NPCs belong to which business. A deleted business's rows go with it. */
public interface BusinessNpcRepository {
    /** @return every saved business NPC */
    List<BusinessNpc> loadAll();

    /** Saves a new business NPC. */
    void save(BusinessNpc npc);

    /** Forgets an NPC that no longer belongs to a business. */
    void delete(UUID npc);
}
