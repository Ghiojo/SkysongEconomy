package org.skysongdev.skysongEconomy.business.npc;

import org.skysongdev.skysongEconomy.business.Business;

/** Tells other plugins about business NPCs. On the server these are Bukkit events. */
public interface BusinessNpcEvents {
    /** Nobody is told. */
    BusinessNpcEvents NONE = new BusinessNpcEvents() {
        @Override
        public void created(Business business, BusinessNpc npc) {
        }

        @Override
        public void removed(Business business, BusinessNpc npc, RemovalCause cause) {
        }
    };

    /** Why a business no longer has an NPC. */
    enum RemovalCause {
        /** Staff dismissed it with {@code /se business dismissnpc}. */
        DISMISSED,
        /** The business lost the slot it filled: a lower tier, a cleared skill pick, or changed config. */
        SLOT_LOST,
        /** The business was deleted. */
        BUSINESS_DELETED,
        /** It was removed in SkysongNPCs itself, for example with {@code /npc remove}. */
        REMOVED_ELSEWHERE
    }

    /** A business created an NPC; it is waiting for its first setup. */
    void created(Business business, BusinessNpc npc);

    /** A business no longer has an NPC. Unless the cause is {@link RemovalCause#REMOVED_ELSEWHERE}, the NPC is being removed too. */
    void removed(Business business, BusinessNpc npc, RemovalCause cause);
}
