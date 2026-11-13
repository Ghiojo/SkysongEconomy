package org.skysongdev.skysongEconomy.business.npc;

import java.util.UUID;

/** What a business NPC is waiting for, as its owner sees it. */
public enum NpcStatus {
    /** Just created: its first setup (name, ancestry, first choices) is not finished. */
    SETUP("business.npcs.status.setup"),
    /** It has stat points, proficiency picks or ability choices nobody has spent yet. */
    CHOICES("business.npcs.status.choices"),
    /** Nothing is left to choose. */
    READY("business.npcs.status.ready");

    private final String messageKey;

    NpcStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    /** @return the messages.yml key of the status's text */
    public String messageKey() {
        return messageKey;
    }

    /** @return the status of an NPC, {@link #READY} when it no longer exists */
    public static NpcStatus of(NpcGateway npcs, NpcSheetGateway sheets, UUID npc) {
        if (npcs.inSetup(npc)) {
            return SETUP;
        }
        return sheets.pending(npc).any() ? CHOICES : READY;
    }
}
