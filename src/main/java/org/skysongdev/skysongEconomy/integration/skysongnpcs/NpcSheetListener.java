package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongEconomy.business.npc.benefit.NpcProgression;
import org.skysongdev.skysongnpcs.api.event.NpcCloneEvent;
import org.skysongdev.skysongnpcs.api.event.NpcRespecEvent;

/**
 * Keeps business NPCs in line when SkysongNPCs changes them on its own: after a respec the NPC
 * is checked again, and a copy of a business NPC gives back what the business gave the original.
 */
public final class NpcSheetListener implements Listener {
    private final NpcProgression progression;

    public NpcSheetListener(NpcProgression progression) {
        this.progression = progression;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespec(NpcRespecEvent event) {
        progression.respecced(event.getNpc());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClone(NpcCloneEvent event) {
        progression.copied(event.getOriginal().id(), event.getCopy().id());
    }
}
