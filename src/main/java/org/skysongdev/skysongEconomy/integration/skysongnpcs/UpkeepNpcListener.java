package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.skysongdev.skysongEconomy.upkeep.UpkeepService;
import org.skysongdev.skysongnpcs.api.event.NpcDeleteEvent;

/** Forgets the upkeep of an NPC that SkysongNPCs deleted for good. */
public final class UpkeepNpcListener implements Listener {
    private final UpkeepService upkeep;

    public UpkeepNpcListener(UpkeepService upkeep) {
        this.upkeep = upkeep;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDelete(NpcDeleteEvent event) {
        upkeep.npcRemoved(event.getNpc().id());
    }
}
