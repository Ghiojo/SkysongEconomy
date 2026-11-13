package org.skysongdev.skysongEconomy.business.module.effect;

import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;
import org.skysongdev.skysongEconomy.business.npc.SlotSource;

/**
 * Which NPCs of a business an {@code npc-benefit} effect is for.
 * <pre>
 * target: all        # every NPC that counts, including ones created later
 * target: 2          # that many, chosen by the owner
 * from: martial      # optional: only the NPCs of that skill's slots count
 * from: module       # optional: only the NPCs of this module's own slots count
 * </pre>
 *
 * @param count how many NPCs the owner chooses; {@link #ALL} for every NPC that counts
 * @param from  the only slot source whose NPCs count, {@code null} when every NPC of the business does
 */
public record NpcTarget(int count, @Nullable SlotSource from) {
    /** The {@link #count} that means every NPC that counts. */
    public static final int ALL = 0;

    public NpcTarget {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
    }

    /** @return whether every NPC that counts receives the benefit, with nothing for the owner to choose */
    public boolean isAll() {
        return count == ALL;
    }

    /** @return whether the NPC is one that counts */
    public boolean counts(BusinessNpc npc) {
        return from == null || from.equals(npc.source());
    }
}
