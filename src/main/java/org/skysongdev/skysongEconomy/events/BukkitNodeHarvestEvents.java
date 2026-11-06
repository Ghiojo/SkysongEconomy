package org.skysongdev.skysongEconomy.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.NodeHarvestEvents;

/** Fires {@link NodeHarvestEvent}. */
public final class BukkitNodeHarvestEvents implements NodeHarvestEvents {
    private final PluginManager plugins;

    public BukkitNodeHarvestEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void harvested(Harvest harvest) {
        Player player = Bukkit.getPlayer(harvest.character().player());
        // Harvests happen from a menu, so the player is online.
        if (player != null) {
            plugins.callEvent(new NodeHarvestEvent(player, harvest));
        }
    }
}
