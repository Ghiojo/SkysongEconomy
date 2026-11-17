package org.skysongdev.skysongEconomy.upkeep;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What the upkeep rules need from SkysongNPCs. */
public interface UpkeepNpcs {
    /**
     * A managed NPC.
     *
     * @param id       its Citizens UUID
     * @param template the template it was made from, in lower case; empty for NPCs staff adopted
     */
    record Known(UUID id, Optional<String> template) {
    }

    /** @return every NPC SkysongNPCs manages */
    List<Known> all();

    /** @return whether SkysongNPCs still manages this NPC */
    boolean exists(UUID npc);

    /** @return the NPC's name without formatting; empty when it is not managed */
    Optional<String> name(UUID npc);
}
