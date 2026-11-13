package org.skysongdev.skysongEconomy.business.npc;

import java.util.List;
import java.util.Objects;

/**
 * The NPC slots one source gives a business, and the NPCs filling them.
 *
 * @param source   what gives the slots
 * @param capacity how many slots it gives right now
 * @param npcs     the NPCs in them, oldest first
 */
public record NpcSlotGroup(SlotSource source, int capacity, List<BusinessNpc> npcs) {
    public NpcSlotGroup {
        Objects.requireNonNull(source, "source");
        npcs = List.copyOf(npcs);
    }

    /** @return how many more NPCs the source has room for */
    public int free() {
        return Math.max(0, capacity - npcs.size());
    }
}
