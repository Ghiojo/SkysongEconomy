package org.skysongdev.skysongEconomy.business.npc.benefit;

import java.util.Map;
import java.util.UUID;

/** Where it is saved what the business system changed on each NPC's sheet. Rows go with the business NPC. */
public interface NpcAppliedRepository {
    /** @return what was changed on every NPC's sheet, by NPC id */
    Map<UUID, NpcApplied> loadAll();

    /** Replaces what is saved for one NPC; saving {@link NpcApplied#NONE} forgets it. */
    void save(UUID npc, NpcApplied applied);
}
