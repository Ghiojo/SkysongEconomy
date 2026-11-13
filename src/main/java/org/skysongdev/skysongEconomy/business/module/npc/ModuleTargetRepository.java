package org.skysongdev.skysongEconomy.business.module.npc;

import java.util.List;
import java.util.UUID;

/**
 * Where it is saved which NPCs an owner chose for a module's benefit. A deleted business's rows
 * go with it.
 */
public interface ModuleTargetRepository {
    /**
     * One chosen NPC.
     *
     * @param businessId the business
     * @param pickOrder  the module pick's number within the business
     * @param effect     which of the module's effects, counted from 0
     * @param npc        the chosen NPC
     */
    record Row(String businessId, int pickOrder, int effect, UUID npc) {
    }

    /** @return every saved choice, in the order they were made */
    List<Row> loadAll();

    /** Saves a new choice. */
    void save(Row row);

    /** Forgets every choice of one module pick. */
    void deletePick(String businessId, int pickOrder);

    /** Forgets every choice of an NPC that is gone. */
    void deleteNpc(UUID npc);
}
