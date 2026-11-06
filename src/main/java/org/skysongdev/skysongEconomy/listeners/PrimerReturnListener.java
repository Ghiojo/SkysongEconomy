package org.skysongdev.skysongEconomy.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongEconomy.nodes.farm.PrimerReturns;

/** Gives a joining player the primers that went back to them while they were offline. */
public final class PrimerReturnListener implements Listener {
    private final PrimerReturns returns;

    public PrimerReturnListener(PrimerReturns returns) {
        this.returns = returns;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        returns.deliverWaiting(event.getPlayer().getUniqueId());
    }
}
