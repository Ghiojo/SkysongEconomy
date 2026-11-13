package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongEconomy.business.npc.BusinessNpcs;
import org.skysongdev.skysongnpcs.api.event.NpcDeleteEvent;

/**
 * Frees a business's NPC slot when its NPC is removed or released in SkysongNPCs itself, for
 * example with {@code /npc remove}. Removals the business system started are already forgotten
 * by the time SkysongNPCs announces them, so they pass through here unnoticed.
 */
public final class NpcRemovalListener implements Listener {
    private final BusinessNpcs businessNpcs;

    public NpcRemovalListener(BusinessNpcs businessNpcs) {
        this.businessNpcs = businessNpcs;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDelete(NpcDeleteEvent event) {
        businessNpcs.removedElsewhere(event.getNpc().id());
    }
}
