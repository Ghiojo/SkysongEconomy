package org.skysongdev.skysongEconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongEconomy.gui.Menu;
import org.skysongdev.skysongEconomy.integration.skysongitems.UpkeepItemMarks;
import org.skysongdev.skysongEconomy.upkeep.UpkeepChests;
import org.skysongdev.skysongnpcs.api.event.NpcPickupEvent;
import org.skysongdev.skysongnpcs.api.event.NpcPlaceEvent;

/**
 * Brings NPC items up to date with their NPC's upkeep wherever players handle them: a moment
 * after a pickup, when a player joins, when a container opens (its items and the player's), and
 * when an inventory closes, such as after a crafting menu repaired an item. An item that places
 * its NPC is checked for a repair first. {@code UpkeepFeature} also sweeps online players' inventories
 * every check.
 */
public final class UpkeepItemListener implements Listener {
    private final Plugin plugin;
    private final UpkeepItemMarks marks;

    public UpkeepItemListener(Plugin plugin, UpkeepItemMarks marks) {
        this.plugin = plugin;
        this.marks = marks;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        later(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(NpcPickupEvent event) {
        // The item is handed over after the event.
        later(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(NpcPlaceEvent event) {
        if (event.getNpc().isPresent()) {
            marks.checkRepair(event.getItem());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Object holder = event.getInventory().getHolder();
        // This plugin's own menus and upkeep chests never hold NPC items worth changing.
        if (!(holder instanceof Menu) && !(holder instanceof UpkeepChests.Chest)) {
            marks.sync(event.getInventory(), player);
        }
        marks.sync(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            later(player);
        }
    }

    private void later(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                marks.sync(player);
            }
        });
    }
}
