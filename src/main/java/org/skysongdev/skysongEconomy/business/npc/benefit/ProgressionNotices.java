package org.skysongdev.skysongEconomy.business.npc.benefit;

import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpc;

/** Tells people when a business NPC received something. On the server the owner gets a message. */
@FunctionalInterface
public interface ProgressionNotices {
    /** Nobody is told. */
    ProgressionNotices NONE = (business, npc, changes) -> {
    };

    /**
     * An NPC that already existed was brought in line with its business and something changed.
     *
     * @param changes what changed; see {@link Changes#leavesChoices()} for whether the owner has
     *                something to choose
     */
    void changed(Business business, BusinessNpc npc, Changes changes);
}
